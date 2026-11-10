package com.projects.kapikua_api.billetera.application.service;

import com.projects.kapikua_api.billetera.application.port.in.Comprobante;
import com.projects.kapikua_api.billetera.application.port.in.EnviarDineroUseCase;
import com.projects.kapikua_api.billetera.application.port.out.BilleteraRepositoryPort;
import com.projects.kapikua_api.billetera.application.port.out.ParametrosRegulatorios;
import com.projects.kapikua_api.billetera.application.port.out.Titulares;
import com.projects.kapikua_api.billetera.application.port.out.TransaccionRepositoryPort;
import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.billetera.domain.model.Billetera;
import com.projects.kapikua_api.billetera.domain.model.Celular;
import com.projects.kapikua_api.billetera.domain.model.TipoTransaccion;
import com.projects.kapikua_api.billetera.domain.model.Topes;
import com.projects.kapikua_api.billetera.domain.service.TopesSBS;
import com.projects.kapikua_api.billetera.domain.model.Transaccion;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.puerto.EjecutorTransaccional;
import com.projects.kapikua_api.shared.puerto.CodigoUnSoloUso;
import com.projects.kapikua_api.shared.puerto.PublicadorEventos;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Service
public class EnviarDineroService implements EnviarDineroUseCase {
    private final BilleteraRepositoryPort billeteras;
    private final TransaccionRepositoryPort transacciones;
    private final ParametrosRegulatorios parametros;
    private final Titulares titulares;
    private final CodigoUnSoloUso codigos;
    private final PublicadorEventos publicador;
    private final EjecutorTransaccional ejecutor;
    private final ArmadorComprobantes comprobantes;
    private final Clock clock;

    EnviarDineroService(BilleteraRepositoryPort billeteras, TransaccionRepositoryPort transacciones,
                        ParametrosRegulatorios parametros, Titulares titulares, CodigoUnSoloUso codigos,
                        PublicadorEventos publicador, EjecutorTransaccional ejecutor,
                        ArmadorComprobantes comprobantes, Clock clock) {
        this.billeteras = billeteras;
        this.transacciones = transacciones;
        this.parametros = parametros;
        this.titulares = titulares;
        this.codigos = codigos;
        this.publicador = publicador;
        this.ejecutor = ejecutor;
        this.comprobantes = comprobantes;
        this.clock = clock;
    }

    @Override
    public Result<VistaPrevia, ErrorBilletera> prepararTransferencia(UsuarioId origen, String celularDestino,
                                                                     Dinero monto, String referencia) {
        return validar(TipoTransaccion.TRANSFERENCIA, origen, celularDestino, monto).map(partes -> {
            var codigo = codigos.generar(origen, CodigoUnSoloUso.Proposito.TRANSFERENCIA,
                    detalle(partes.destino().getCelular(), monto, referencia));
            return new VistaPrevia(titulares.nombreEnmascarado(partes.destino().getUsuarioId()),
                    partes.destino().getCelular().enmascarado(), monto, referencia, codigo);
        });
    }

    @Override
    public Result<Comprobante, ErrorBilletera> transferir(UsuarioId origen, String celularDestino, Dinero monto,
                                                          String referencia, String codigo) {
        var repetida = comprobanteSiYaExiste(origen, referencia);
        if (repetida != null) return repetida;

        var celular = Celular.de(celularDestino);
        if (celular.isEmpty()) return Result.err(new ErrorBilletera.CelularInvalido());
        var validacion = codigos.validar(origen, CodigoUnSoloUso.Proposito.TRANSFERENCIA,
                detalle(celular.get(), monto, referencia), codigo);
        if (validacion instanceof Result.Err<Void, CodigoUnSoloUso.CodigoError> err) {
            return Result.err(new ErrorBilletera.CodigoNoValido(err.error().mensaje()));
        }
        return enviar(TipoTransaccion.TRANSFERENCIA, origen, celularDestino, monto, referencia);
    }

    @Override
    public Result<VistaPrevia, ErrorBilletera> prepararPagoNegocio(UsuarioId origen, String celularNegocio,
                                                                   Dinero monto, String referencia) {
        return validar(TipoTransaccion.PAGO_NEGOCIO, origen, celularNegocio, monto).map(partes ->
                new VistaPrevia(partes.destino().getNombreNegocio(), partes.destino().getCelular().enmascarado(),
                        monto, referencia, null));
    }

    @Override
    public Result<Comprobante, ErrorBilletera> pagarNegocio(UsuarioId origen, String celularNegocio, Dinero monto,
                                                            String referencia) {
        var repetida = comprobanteSiYaExiste(origen, referencia);
        if (repetida != null) return repetida;
        return enviar(TipoTransaccion.PAGO_NEGOCIO, origen, celularNegocio, monto, referencia);
    }

    private record Partes(Billetera origen, Billetera destino, Topes topes) {}

    private Result<Partes, ErrorBilletera> validar(TipoTransaccion tipo, UsuarioId usuarioOrigen,
                                                   String celularDestino, Dinero monto) {
        var celular = Celular.de(celularDestino);
        if (celular.isEmpty()) return Result.err(new ErrorBilletera.CelularInvalido());
        var origen = billeteras.porUsuario(usuarioOrigen);
        if (origen.isEmpty()) return Result.err(new ErrorBilletera.BilleteraNoEncontrada());
        var destino = billeteras.porCelular(celular.get());
        if (destino.isEmpty()) {
            return Result.err(tipo == TipoTransaccion.PAGO_NEGOCIO ? new ErrorBilletera.NoEsNegocio()
                                                                   : new ErrorBilletera.DestinatarioNoEncontrado());
        }
        if (origen.get().getId().equals(destino.get().getId())) {
            return Result.err(new ErrorBilletera.OperacionConSiMismo());
        }
        if (tipo == TipoTransaccion.PAGO_NEGOCIO && !destino.get().esNegocio()) {
            return Result.err(new ErrorBilletera.NoEsNegocio());
        }
        var topes = parametros.topesVigentes(LocalDate.now(clock));
        if (topes instanceof Result.Err<Topes, ErrorBilletera> err) return Result.err(err.error());

        var t = ((Result.Ok<Topes, ErrorBilletera>) topes).valor();
        var mes = PeriodoMensual.actual(clock);
        var acumulado = transacciones.acumulado(origen.get().getId(), mes.desde(), mes.hasta());
        return TopesSBS.validarEnvio(origen.get(), destino.get(), monto, acumulado, t)
                .map(ok -> new Partes(origen.get(), destino.get(), t));
    }

    private Result<Comprobante, ErrorBilletera> enviar(TipoTransaccion tipo, UsuarioId usuarioOrigen,
                                                       String celularDestino, Dinero monto, String referencia) {
        try {
            return ejecutor.ejecutar(() -> validar(tipo, usuarioOrigen, celularDestino, monto)
                    .flatMap(partes -> mover(tipo, partes, monto, referencia)));
        } catch (DataIntegrityViolationException e) {
            var repetida = comprobanteSiYaExiste(usuarioOrigen, referencia);
            if (repetida != null) return repetida;
            throw e;
        } catch (OptimisticLockingFailureException e) {
            return Result.err(new ErrorBilletera.OperacionConcurrente());
        }
    }

    private Result<Comprobante, ErrorBilletera> mover(TipoTransaccion tipo, Partes partes, Dinero monto,
                                                      String referencia) {
        var origen = partes.origen();
        var destino = partes.destino();
        var debito = origen.debitar(monto);
        if (debito instanceof Result.Err<Void, ErrorBilletera> err) return Result.err(err.error());
        var credito = destino.acreditar(monto, partes.topes().saldoMaximo(), true);
        if (credito instanceof Result.Err<Void, ErrorBilletera> err) return Result.err(err.error());

        Billetera origenGuardado;
        Billetera destinoGuardado;
        if (origen.getId() < destino.getId()) {
            origenGuardado = billeteras.guardar(origen);
            destinoGuardado = billeteras.guardar(destino);
        } else {
            destinoGuardado = billeteras.guardar(destino);
            origenGuardado = billeteras.guardar(origen);
        }

        var ahora = OffsetDateTime.now(clock);
        var descripcion = tipo == TipoTransaccion.PAGO_NEGOCIO ? destino.getNombreNegocio() : null;
        var transaccion = transacciones.registrar(Transaccion.entreBilleteras(
                tipo, origenGuardado, destinoGuardado, monto, referencia, descripcion, ahora));

        publicador.publicarTodos(transaccion.eventos());

        return Result.ok(new Comprobante(transaccion.id(), tipo, monto, referencia,
                comprobantes.contraparte(tipo, destino, false), ahora, origenGuardado.saldoDisponible()));
    }

    private Result<Comprobante, ErrorBilletera> comprobanteSiYaExiste(UsuarioId usuario, String referencia) {
        return transacciones.porReferencia(referencia)
                .map(t -> comprobantes.paraOrigen(t, usuario)
                        .<Result<Comprobante, ErrorBilletera>>map(Result::ok)
                        .orElseGet(() -> Result.err(new ErrorBilletera.ReferenciaInvalida())))
                .orElse(null);
    }

    private static String detalle(Celular destino, Dinero monto, String referencia) {
        return destino.numero() + "|" + monto.valor().toPlainString() + "|" + referencia;
    }
}
