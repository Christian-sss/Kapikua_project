package com.projects.kapikua_api.credito.application.service;

import com.projects.kapikua_api.credito.application.port.in.GestionarAvalUseCase;
import com.projects.kapikua_api.credito.application.port.out.ClientesCredito;
import com.projects.kapikua_api.credito.application.port.out.EvaluacionCrediticia;
import com.projects.kapikua_api.credito.application.port.out.OfertaRepositoryPort;
import com.projects.kapikua_api.credito.application.port.out.OperacionesBilletera;
import com.projects.kapikua_api.credito.application.port.out.PrestamoRepositoryPort;
import com.projects.kapikua_api.credito.application.port.out.ReglasCredito;
import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.credito.domain.model.Oferta;
import com.projects.kapikua_api.evaluacion.application.port.in.ResultadoEvaluacion;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.puerto.EjecutorTransaccional;
import com.projects.kapikua_api.shared.puerto.CodigoUnSoloUso;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class AvalService implements GestionarAvalUseCase {
    private final SoporteOfertas soporte;
    private final DecisionEntidad decision;
    private final OfertaRepositoryPort ofertas;
    private final PrestamoRepositoryPort prestamos;
    private final EvaluacionCrediticia evaluacion;
    private final OperacionesBilletera billetera;
    private final ClientesCredito clientes;
    private final CodigoUnSoloUso codigos;
    private final EjecutorTransaccional ejecutor;
    private final Clock clock;

    AvalService(SoporteOfertas soporte, DecisionEntidad decision, OfertaRepositoryPort ofertas,
                PrestamoRepositoryPort prestamos, EvaluacionCrediticia evaluacion, OperacionesBilletera billetera,
                ClientesCredito clientes, CodigoUnSoloUso codigos, EjecutorTransaccional ejecutor, Clock clock) {
        this.soporte = soporte;
        this.decision = decision;
        this.ofertas = ofertas;
        this.prestamos = prestamos;
        this.evaluacion = evaluacion;
        this.billetera = billetera;
        this.clientes = clientes;
        this.codigos = codigos;
        this.ejecutor = ejecutor;
        this.clock = clock;
    }

    @Override
    public Result<Void, ErrorCredito> designar(UsuarioId deudor, Long ofertaId, String celularAval) {
        var cargada = soporte.actualizada(ofertaId, deudor);
        if (cargada instanceof Result.Err<Oferta, ErrorCredito> err) return Result.err(err.error());
        var oferta = ((Result.Ok<Oferta, ErrorCredito>) cargada).valor();
        if (!oferta.getUsuarioId().equals(deudor)) return Result.err(new ErrorCredito.OfertaNoEncontrada());
        var ev = soporte.evaluacionDe(oferta);
        if (ev instanceof Result.Err<ResultadoEvaluacion, ErrorCredito> err) return Result.err(err.error());
        var requiereAval = ((Result.Ok<ResultadoEvaluacion, ErrorCredito>) ev).valor().requiereAval();

        var aval = billetera.usuarioPorCelular(celularAval);
        if (aval.isEmpty()) return Result.err(new ErrorCredito.AvalNoEncontrado());
        var avalId = aval.get();
        if (avalId.equals(deudor)) return Result.err(new ErrorCredito.AvalMismoDeudor());
        if (ofertas.esAvalEnCursoDe(deudor, avalId)) return Result.err(new ErrorCredito.AvalCruzado());
        if (!evaluacion.evaluarAval(avalId).esOk()
                || ofertas.tieneGarantiaActiva(avalId)
                || prestamos.tienePrestamoEnMora(avalId)) {
            return Result.err(new ErrorCredito.AvalNoCalifica());
        }

        var reglas = soporte.reglas();
        if (reglas instanceof Result.Err<ReglasCredito.Reglas, ErrorCredito> err) return Result.err(err.error());
        var maxAvales = ((Result.Ok<ReglasCredito.Reglas, ErrorCredito>) reglas).valor().maxAvales();
        return ejecutor.ejecutarResultado(() -> {
            var o = ofertas.porId(ofertaId).orElseThrow();
            var designado = o.designarAval(avalId, requiereAval, maxAvales, soporte.ahora());
            if (designado.esOk()) ofertas.guardar(o);
            return designado.map(a -> null);
        });
    }

    @Override
    public Result<InvitacionAval, ErrorCredito> invitacion(UsuarioId aval, Long ofertaId) {
        var cargada = soporte.actualizada(ofertaId, aval);
        if (cargada instanceof Result.Err<Oferta, ErrorCredito> err) return Result.err(err.error());
        var oferta = ((Result.Ok<Oferta, ErrorCredito>) cargada).valor();
        var invitacion = oferta.invitacionPendientePara(aval);
        if (invitacion.isEmpty()) return Result.err(new ErrorCredito.InvitacionNoEncontrada());
        var ev = soporte.evaluacionDe(oferta);
        if (ev instanceof Result.Err<ResultadoEvaluacion, ErrorCredito> err) return Result.err(err.error());
        var c = ((Result.Ok<ResultadoEvaluacion, ErrorCredito>) ev).valor().condiciones();
        var plazo = ((Result.Ok<ReglasCredito.Reglas, ErrorCredito>) soporte.reglas()).valor().plazoRespuestaAval();

        var codigo = codigos.generar(aval, CodigoUnSoloUso.Proposito.ACEPTAR_AVAL, detalle(ofertaId));
        return Result.ok(new InvitacionAval(ofertaId, clientes.nombreEnmascarado(oferta.getUsuarioId()), c.monto(),
                c.cuotas(), c.cuota(), c.tea(), c.porcentajeRetencion(),
                invitacion.get().venceEn(plazo, oferta.finVigencia(clock.getZone())), codigo));
    }

    @Override
    public Result<Void, ErrorCredito> responder(UsuarioId aval, Long ofertaId, boolean acepta, String codigo,
                                                String dispositivo) {
        var cargada = soporte.actualizada(ofertaId, aval);
        if (cargada instanceof Result.Err<Oferta, ErrorCredito> err) return Result.err(err.error());
        var oferta = ((Result.Ok<Oferta, ErrorCredito>) cargada).valor();
        if (oferta.invitacionPendientePara(aval).isEmpty()) return Result.err(new ErrorCredito.InvitacionNoEncontrada());
        var ev = soporte.evaluacionDe(oferta);
        if (ev instanceof Result.Err<ResultadoEvaluacion, ErrorCredito> err) return Result.err(err.error());
        var porcentaje = ((Result.Ok<ResultadoEvaluacion, ErrorCredito>) ev).valor().condiciones().porcentajeRetencion();

        if (acepta) {
            var valido = codigos.validar(aval, CodigoUnSoloUso.Proposito.ACEPTAR_AVAL, detalle(ofertaId), codigo);
            if (valido instanceof Result.Err<Void, CodigoUnSoloUso.CodigoError> err) {
                return Result.err(new ErrorCredito.CodigoNoValido(err.error().mensaje()));
            }
            if (ofertas.tieneGarantiaActiva(aval)) return Result.err(new ErrorCredito.AvalNoCalifica());
            clientes.otorgarConsentimientoDeGarantia(aval);
        }
        var respuesta = ejecutor.ejecutarResultado(() -> {
            var o = ofertas.porId(ofertaId).orElseThrow();
            var r = o.responderAval(aval, acepta, porcentaje, dispositivo, soporte.ahora());
            if (r.esOk()) ofertas.guardar(o);
            return r;
        });
        if (acepta && respuesta.esOk()) decision.pedirSiEstaLista(ofertaId);
        return respuesta;
    }

    private static String detalle(Long ofertaId) {
        return "oferta|" + ofertaId;
    }
}
