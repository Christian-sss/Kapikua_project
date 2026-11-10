package com.projects.kapikua_api.billetera.application.service;

import com.projects.kapikua_api.billetera.application.port.in.OperacionesCreditoUseCase;
import com.projects.kapikua_api.billetera.application.port.in.OperacionesCreditoUseCase.Cargo;
import com.projects.kapikua_api.billetera.application.port.out.BilleteraRepositoryPort;
import com.projects.kapikua_api.billetera.application.port.out.ParametrosRegulatorios;
import com.projects.kapikua_api.billetera.application.port.out.TransaccionRepositoryPort;
import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.billetera.domain.model.Billetera;
import com.projects.kapikua_api.billetera.domain.model.TipoTransaccion;
import com.projects.kapikua_api.billetera.domain.model.Topes;
import com.projects.kapikua_api.billetera.domain.model.Transaccion;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.puerto.PublicadorEventos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Service
public class OperacionesCreditoService implements OperacionesCreditoUseCase {

    private final BilleteraRepositoryPort billeteras;
    private final TransaccionRepositoryPort transacciones;
    private final ParametrosRegulatorios parametros;
    private final PublicadorEventos publicador;
    private final Clock clock;

    public OperacionesCreditoService(BilleteraRepositoryPort billeteras, TransaccionRepositoryPort transacciones,
                                     ParametrosRegulatorios parametros, PublicadorEventos publicador, Clock clock) {
        this.billeteras = billeteras;
        this.transacciones = transacciones;
        this.parametros = parametros;
        this.publicador = publicador;
        this.clock = clock;
    }

    @Override
    public Result<Void, ErrorBilletera> verificarIngreso(UsuarioId usuarioId, Dinero monto) {
        return billeteraYTopes(usuarioId, monto).map(b -> null);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Result<Long, ErrorBilletera> acreditarDesembolso(UsuarioId usuarioId, Dinero monto, String referencia,
                                                            String descripcion) {
        // Idempotente: el mismo desembolso no entra dos veces (RNF-07).
        var existente = transacciones.porReferencia(referencia);
        if (existente.isPresent()) return Result.ok(existente.get().id());

        var validacion = billeteraYTopes(usuarioId, monto);
        if (validacion instanceof Result.Err<Billetera, ErrorBilletera> err) return Result.err(err.error());
        var billetera = ((Result.Ok<Billetera, ErrorBilletera>) validacion).valor();

        var guardada = billeteras.guardar(billetera);   // ya tiene el monto acreditado en memoria
        var ahora = OffsetDateTime.now(clock);
        var transaccion = transacciones.registrar(Transaccion.desembolso(guardada, monto, referencia, descripcion, ahora));
        publicador.publicarTodos(transaccion.eventos());
        return Result.ok(transaccion.id());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Result<Dinero, ErrorBilletera> apartar(UsuarioId usuarioId, Dinero monto) {
        var billetera = billeteras.porUsuario(usuarioId);
        if (billetera.isEmpty()) return Result.err(new ErrorBilletera.BilleteraNoEncontrada());
        var apartado = billetera.get().apartar(monto);
        if (!apartado.esCero()) billeteras.guardar(billetera.get());
        return Result.ok(apartado);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Result<Void, ErrorBilletera> liberarApartado(UsuarioId usuarioId, Dinero monto) {
        if (monto.esCero()) return Result.ok();
        var billetera = billeteras.porUsuario(usuarioId);
        if (billetera.isEmpty()) return Result.err(new ErrorBilletera.BilleteraNoEncontrada());
        billetera.get().liberarApartado(monto);
        billeteras.guardar(billetera.get());
        return Result.ok();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Result<Cargo, ErrorBilletera> cobrar(UsuarioId usuarioId, Dinero monto, Dinero usarApartado, boolean exigirTodo,
                                                boolean anticipado, String referencia, String descripcion) {
        // Idempotente (RNF-07): el mismo cobro no se hace dos veces; se avisa que no es nuevo.
        var existente = transacciones.porReferencia(referencia);
        if (existente.isPresent()) {
            return Result.ok(new Cargo(existente.get().id(), existente.get().monto(), Dinero.CERO, false));
        }
        var encontrada = billeteras.porUsuario(usuarioId);
        if (encontrada.isEmpty()) return Result.err(new ErrorBilletera.BilleteraNoEncontrada());
        var b = encontrada.get();

        var deApartado = menor(menor(usarApartado, b.getSaldoApartado()), monto);
        var deDisponible = menor(monto.restar(deApartado), b.saldoDisponible());
        var cobrado = deApartado.sumar(deDisponible);
        if (exigirTodo && cobrado.menorQue(monto)) return Result.err(new ErrorBilletera.SaldoInsuficiente());
        if (cobrado.esCero()) return Result.ok(Cargo.NADA);

        b.consumirApartado(deApartado);
        if (!deDisponible.esCero()) {
            var debito = b.debitar(deDisponible);
            if (debito instanceof Result.Err<Void, ErrorBilletera> err) return Result.err(err.error());
        }
        var guardada = billeteras.guardar(b);
        var ahora = OffsetDateTime.now(clock);
        var tipo = anticipado ? TipoTransaccion.PAGO_ANTICIPADO : TipoTransaccion.PAGO_CUOTA;
        var transaccion = transacciones.registrar(Transaccion.pagoACredito(tipo, guardada, cobrado, referencia,
                descripcion, ahora));
        publicador.publicarTodos(transaccion.eventos());
        return Result.ok(new Cargo(transaccion.id(), cobrado, deApartado, true));
    }

    private static Dinero menor(Dinero a, Dinero b) {
        return a.menorQue(b) ? a : b;
    }

    /** RN-01 (tope por operación) y RN-02 (saldo máximo). Devuelve la billetera con el monto ya acreditado en memoria. */
    private Result<Billetera, ErrorBilletera> billeteraYTopes(UsuarioId usuarioId, Dinero monto) {
        var billetera = billeteras.porUsuario(usuarioId);
        if (billetera.isEmpty()) return Result.err(new ErrorBilletera.BilleteraNoEncontrada());
        var topes = parametros.topesVigentes(LocalDate.now(clock));
        if (topes instanceof Result.Err<Topes, ErrorBilletera> err) return Result.err(err.error());
        var t = ((Result.Ok<Topes, ErrorBilletera>) topes).valor();
        if (monto.mayorQue(t.porOperacion())) return Result.err(new ErrorBilletera.TopePorOperacion(t.porOperacion()));
        return billetera.get().acreditar(monto, t.saldoMaximo(), false).map(ok -> billetera.get());
    }
}
