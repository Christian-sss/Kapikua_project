package com.projects.kapikua_api.billetera.service;

import com.projects.kapikua_api.billetera.dto.Comprobante;
import com.projects.kapikua_api.billetera.dto.VistaPrevia;
import com.projects.kapikua_api.billetera.repository.BilleteraRepository;
import com.projects.kapikua_api.billetera.repository.TransaccionRepository;
import com.projects.kapikua_api.billetera.error.ErrorBilletera;
import com.projects.kapikua_api.billetera.model.Billetera;
import com.projects.kapikua_api.billetera.model.Celular;
import com.projects.kapikua_api.billetera.model.TipoTransaccion;
import com.projects.kapikua_api.billetera.model.TiposTransaccion;
import com.projects.kapikua_api.billetera.model.Topes;
import com.projects.kapikua_api.billetera.model.Transaccion;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.identidad.service.IdentidadService;
import com.projects.kapikua_api.shared.puerto.EjecutorTransaccional;
import com.projects.kapikua_api.shared.puerto.CodigoUnSoloUso;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Transferencias entre personas y pagos a negocios (con vista previa y comprobante). */
@Service
public class EnvioService {
    private final BilleteraRepository billeteras;
    private final TransaccionRepository transacciones;
    private final TopesService topes;
    private final IdentidadService identidad;
    private final CodigoUnSoloUso codigos;
    private final PublicadorTransacciones publicador;
    private final EjecutorTransaccional ejecutor;
    private final ArmadorComprobantes comprobantes;
    private final Clock clock;

    public EnvioService(BilleteraRepository billeteras, TransaccionRepository transacciones,
                        TopesService topes, IdentidadService identidad, CodigoUnSoloUso codigos,
                        PublicadorTransacciones publicador, EjecutorTransaccional ejecutor,
                        ArmadorComprobantes comprobantes, Clock clock) {
        this.billeteras = billeteras;
        this.transacciones = transacciones;
        this.topes = topes;
        this.identidad = identidad;
        this.codigos = codigos;
        this.publicador = publicador;
        this.ejecutor = ejecutor;
        this.comprobantes = comprobantes;
        this.clock = clock;
    }

    public Result<VistaPrevia, ErrorBilletera> prepararTransferencia(UsuarioId origen, String celularDestino,
                                                                     Dinero monto, String referencia) {
        return preparar(TiposTransaccion.TRANSFERENCIA, origen, celularDestino, monto, referencia);
    }

    public Result<Comprobante, ErrorBilletera> transferir(UsuarioId origen, String celularDestino, Dinero monto,
                                                          String referencia, String codigo) {
        return confirmar(TiposTransaccion.TRANSFERENCIA, origen, celularDestino, monto, referencia, codigo);
    }

    public Result<VistaPrevia, ErrorBilletera> prepararPagoNegocio(UsuarioId origen, String celularNegocio,
                                                                   Dinero monto, String referencia) {
        return preparar(TiposTransaccion.PAGO_NEGOCIO, origen, celularNegocio, monto, referencia);
    }

    public Result<Comprobante, ErrorBilletera> pagarNegocio(UsuarioId origen, String celularNegocio, Dinero monto,
                                                            String referencia) {
        return confirmar(TiposTransaccion.PAGO_NEGOCIO, origen, celularNegocio, monto, referencia, null);
    }

    private Result<VistaPrevia, ErrorBilletera> preparar(TipoTransaccion tipo, UsuarioId origen, String celularDestino,
                                                       Dinero monto, String referencia) {
        return validar(tipo, origen, celularDestino, monto).map(partes -> {
            var destino = partes.destino();
            var codigo = tipo.requiereCodigoDeUnUso()
                    ? codigos.generar(origen, CodigoUnSoloUso.Proposito.TRANSFERENCIA,
                            detalle(destino.getCelular(), monto, referencia)) : null;
            var nombre = tipo.descripcion(destino);
            if (nombre == null) nombre = identidad.nombreEnmascarado(destino.getUsuarioId());
            return new VistaPrevia(nombre, destino.getCelular().enmascarado(), monto, referencia, codigo);
        });
    }

    private Result<Comprobante, ErrorBilletera> confirmar(TipoTransaccion tipo, UsuarioId origen, String celularDestino,
                                                         Dinero monto, String referencia, String codigo) {
        var repetida = comprobanteSiYaExiste(origen, referencia);
        if (repetida != null) return repetida;

        if (tipo.requiereCodigoDeUnUso()) {
            var celular = Celular.de(celularDestino);
            if (celular.isEmpty()) return Result.err(new ErrorBilletera.CelularInvalido());
            var validacion = codigos.validar(origen, CodigoUnSoloUso.Proposito.TRANSFERENCIA,
                    detalle(celular.get(), monto, referencia), codigo);
            if (validacion instanceof Result.Err<Void, CodigoUnSoloUso.CodigoError> err) {
                return Result.err(new ErrorBilletera.CodigoNoValido(err.error().mensaje()));
            }
        }
        return enviar(tipo, origen, celularDestino, monto, referencia);
    }

    private record Partes(Billetera origen, Billetera destino, Topes topes) {
    }

    private Result<Partes, ErrorBilletera> validar(TipoTransaccion tipo, UsuarioId usuarioOrigen,
                                                   String celularDestino, Dinero monto) {
        var celular = Celular.de(celularDestino);
        if (celular.isEmpty()) return Result.err(new ErrorBilletera.CelularInvalido());
        var origen = billeteras.porUsuario(usuarioOrigen);
        if (origen.isEmpty()) return Result.err(new ErrorBilletera.BilleteraNoEncontrada());
        var destino = billeteras.porCelular(celular.get());
        if (destino.isEmpty()) {
            return Result.err(tipo.destinatarioNoEncontrado());
        }
        if (origen.get().getId().equals(destino.get().getId())) {
            return Result.err(new ErrorBilletera.OperacionConSiMismo());
        }
        var destinatario = tipo.validarDestinatario(destino.get());
        if (destinatario instanceof Result.Err<Void, ErrorBilletera> err) {
            return Result.err(err.error());
        }
        var vigentes = topes.topesVigentes(LocalDate.now(clock));
        if (vigentes instanceof Result.Err<Topes, ErrorBilletera> err) return Result.err(err.error());

        var t = ((Result.Ok<Topes, ErrorBilletera>) vigentes).valor();
        var mes = PeriodoMensual.actual(clock);
        var acumulado = transacciones.acumulado(origen.get().getId(), mes.desde(), mes.hasta());
        return TopesSBS.validarEnvio(tipo, origen.get(), destino.get(), monto, acumulado, t)
                .map(ok -> new Partes(origen.get(), destino.get(), t));
    }

    private Result<Comprobante, ErrorBilletera> enviar(TipoTransaccion tipo, UsuarioId usuarioOrigen,
                                                       String celularDestino, Dinero monto, String referencia) {
        try {
            return ejecutor.ejecutarResultado(() -> validar(tipo, usuarioOrigen, celularDestino, monto)
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
        var descripcion = tipo.descripcion(destino);
        var transaccion = transacciones.registrar(Transaccion.entreBilleteras(
                tipo, origenGuardado, destinoGuardado, monto, referencia, descripcion, ahora));

        publicador.publicar(transaccion);

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
