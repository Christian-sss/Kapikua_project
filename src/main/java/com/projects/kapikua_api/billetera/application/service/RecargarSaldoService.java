package com.projects.kapikua_api.billetera.application.service;

import com.projects.kapikua_api.billetera.application.port.in.Comprobante;
import com.projects.kapikua_api.billetera.application.port.in.RecargarSaldoUseCase;
import com.projects.kapikua_api.billetera.application.port.out.BilleteraRepositoryPort;
import com.projects.kapikua_api.billetera.application.port.out.ParametrosRegulatorios;
import com.projects.kapikua_api.billetera.application.port.out.PasarelaPagos;
import com.projects.kapikua_api.billetera.application.port.out.Titulares;
import com.projects.kapikua_api.billetera.application.port.out.TransaccionRepositoryPort;
import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.billetera.domain.model.Billetera;
import com.projects.kapikua_api.billetera.domain.model.TipoTransaccion;
import com.projects.kapikua_api.billetera.domain.model.Topes;
import com.projects.kapikua_api.billetera.domain.service.TopesSBS;
import com.projects.kapikua_api.billetera.domain.model.Transaccion;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.puerto.EjecutorTransaccional;
import com.projects.kapikua_api.shared.puerto.PublicadorEventos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * CU-04. Valida los topes, cobra en la pasarela FUERA de la transacción (SAD 2.4) y luego acredita
 * con el identificador del cargo como referencia única (RNF-07), volviendo a validar los topes.
 */
@Service
public class RecargarSaldoService implements RecargarSaldoUseCase {

    private static final Logger log = LoggerFactory.getLogger(RecargarSaldoService.class);

    private final BilleteraRepositoryPort billeteras;
    private final TransaccionRepositoryPort transacciones;
    private final ParametrosRegulatorios parametros;
    private final PasarelaPagos pasarela;
    private final Titulares titulares;
    private final PublicadorEventos publicador;
    private final EjecutorTransaccional ejecutor;
    private final ArmadorComprobantes comprobantes;
    private final Clock clock;

    RecargarSaldoService(BilleteraRepositoryPort billeteras, TransaccionRepositoryPort transacciones,
                         ParametrosRegulatorios parametros, PasarelaPagos pasarela, Titulares titulares,
                         PublicadorEventos publicador, EjecutorTransaccional ejecutor,
                         ArmadorComprobantes comprobantes, Clock clock) {
        this.billeteras = billeteras;
        this.transacciones = transacciones;
        this.parametros = parametros;
        this.pasarela = pasarela;
        this.titulares = titulares;
        this.publicador = publicador;
        this.ejecutor = ejecutor;
        this.comprobantes = comprobantes;
        this.clock = clock;
    }

    @Override
    public Result<Comprobante, ErrorBilletera> recargar(UsuarioId usuarioId, Dinero monto, String tokenTarjeta,
                                                        String referenciaFormulario) {
        // 3. Topes antes de cobrar: si no pasan, la tarjeta no se toca.
        var previa = validar(usuarioId, monto);
        if (previa instanceof Result.Err<Billetera, ErrorBilletera> err) return Result.err(err.error());

        // 4 y 5. Cargo en la pasarela, fuera de la transacción.
        var cargo = pasarela.cargar(monto, tokenTarjeta, titulares.correo(usuarioId), referenciaFormulario);
        if (cargo instanceof Result.Err<String, ErrorBilletera> err) return Result.err(err.error());
        var idCargo = ((Result.Ok<String, ErrorBilletera>) cargo).valor();

        // 6. Acreditar; 6a. con el mismo cargo no se duplica.
        try {
            var resultado = ejecutor.ejecutar(() -> validar(usuarioId, monto).flatMap(b -> acreditar(b, monto, idCargo)));
            if (resultado instanceof Result.Err<Comprobante, ErrorBilletera> err) {
                log.error("Cargo {} cobrado pero no acreditado ({}): requiere devolución manual",
                        idCargo, err.error().codigo());
            }
            return resultado;
        } catch (DataIntegrityViolationException e) {
            return transacciones.porReferencia(idCargo)
                    .<Result<Comprobante, ErrorBilletera>>map(t -> Result.ok(comprobante(t, usuarioId)))
                    .orElseThrow(() -> e);
        } catch (OptimisticLockingFailureException e) {
            log.error("Cargo {} cobrado pero no acreditado (concurrencia): requiere devolución manual", idCargo);
            return Result.err(new ErrorBilletera.OperacionConcurrente());
        }
    }

    private Result<Billetera, ErrorBilletera> validar(UsuarioId usuarioId, Dinero monto) {
        var billetera = billeteras.porUsuario(usuarioId);
        if (billetera.isEmpty()) return Result.err(new ErrorBilletera.BilleteraNoEncontrada());
        var topes = parametros.topesVigentes(LocalDate.now(clock));
        if (topes instanceof Result.Err<Topes, ErrorBilletera> err) return Result.err(err.error());
        var mes = PeriodoMensual.actual(clock);
        var acumulado = transacciones.acumulado(billetera.get().getId(), mes.desde(), mes.hasta());
        var t = ((Result.Ok<Topes, ErrorBilletera>) topes).valor();
        return TopesSBS.validarRecarga(billetera.get(), monto, acumulado, t)
                .flatMap(ok -> billetera.get().acreditar(monto, t.saldoMaximo(), false))
                .map(ok -> billetera.get());
    }

    /** {@code billetera} ya tiene el monto acreditado en memoria (lo hizo {@link #validar}). */
    private Result<Comprobante, ErrorBilletera> acreditar(Billetera billetera, Dinero monto, String idCargo) {
        var existente = transacciones.porReferencia(idCargo);
        if (existente.isPresent()) return Result.ok(comprobante(existente.get(), billetera.getUsuarioId()));

        var guardada = billeteras.guardar(billetera);
        var ahora = OffsetDateTime.now(clock);
        var transaccion = transacciones.registrar(Transaccion.recarga(guardada, monto, idCargo, ahora));
        publicador.publicarTodos(transaccion.eventos());
        return Result.ok(new Comprobante(transaccion.id(), TipoTransaccion.RECARGA, monto, idCargo,
                "Tarjeta", ahora, guardada.saldoDisponible()));
    }

    private Comprobante comprobante(Transaccion t, UsuarioId usuarioId) {
        return comprobantes.paraUsuario(t, usuarioId).orElseThrow(() ->
                new IllegalStateException("El cargo " + t.referencia() + " ya se acreditó en otra billetera"));
    }
}
