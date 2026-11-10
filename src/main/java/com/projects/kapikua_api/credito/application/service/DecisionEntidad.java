package com.projects.kapikua_api.credito.application.service;

import com.projects.kapikua_api.credito.application.port.out.ClientesCredito;
import com.projects.kapikua_api.credito.application.port.out.EntidadFinanciera;
import com.projects.kapikua_api.credito.application.port.out.EntidadFinanciera.RespuestaEfa;
import com.projects.kapikua_api.credito.application.port.out.EntidadFinanciera.SolicitudEfa;
import com.projects.kapikua_api.credito.application.port.out.OfertaRepositoryPort;
import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.credito.domain.model.Oferta;
import com.projects.kapikua_api.evaluacion.application.port.in.ResultadoEvaluacion;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.puerto.EjecutorTransaccional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * RF-CR-03: envía la solicitud, el score y los datos del aval a la entidad aliada y registra la oferta
 * CONFIRMADA o DENEGADA. La llamada a la entidad va FUERA de la transacción (SAD 2.4). Si no responde,
 * la oferta queda PENDIENTE_EFA y el cliente puede reintentar (CU-08, 9b).
 */
@Component
class DecisionEntidad {

    private static final Logger log = LoggerFactory.getLogger(DecisionEntidad.class);

    private final SoporteOfertas soporte;
    private final OfertaRepositoryPort ofertas;
    private final EntidadFinanciera entidad;
    private final ClientesCredito clientes;
    private final EjecutorTransaccional ejecutor;

    DecisionEntidad(SoporteOfertas soporte, OfertaRepositoryPort ofertas, EntidadFinanciera entidad,
                    ClientesCredito clientes, EjecutorTransaccional ejecutor) {
        this.soporte = soporte;
        this.ofertas = ofertas;
        this.entidad = entidad;
        this.clientes = clientes;
        this.ejecutor = ejecutor;
    }

    /** No hace nada si la oferta aún no está lista (por ejemplo, espera al aval). */
    Result<Void, ErrorCredito> pedirSiEstaLista(Long ofertaId) {
        var cargada = soporte.actualizada(ofertaId, null);
        if (cargada instanceof Result.Err<Oferta, ErrorCredito> err) return Result.err(err.error());
        var oferta = ((Result.Ok<Oferta, ErrorCredito>) cargada).valor();
        var ev = soporte.evaluacionDe(oferta);
        if (ev instanceof Result.Err<ResultadoEvaluacion, ErrorCredito> err) return Result.err(err.error());
        var evaluacion = ((Result.Ok<ResultadoEvaluacion, ErrorCredito>) ev).valor();
        if (!oferta.listaParaEfa(evaluacion.requiereAval())) return Result.ok();

        var c = evaluacion.condiciones();
        var solicitud = new SolicitudEfa(clientes.dni(oferta.getUsuarioId()).orElseThrow(), c.monto(), c.cuotas(),
                c.tea(), c.cuota(), evaluacion.score(), evaluacion.recomendacion().name(),
                oferta.avalAceptado().flatMap(a -> clientes.dni(a.getUsuarioAvalId())).orElse(null));
        var respuesta = entidad.solicitarDecision(solicitud);
        if (respuesta instanceof Result.Err<RespuestaEfa, ErrorCredito> err) {
            log.warn("La entidad aliada no respondió para la oferta {}: queda PENDIENTE_EFA", ofertaId);
            return Result.err(err.error());
        }
        var decision = ((Result.Ok<RespuestaEfa, ErrorCredito>) respuesta).valor();
        return ejecutor.ejecutarResultado(() -> {
            var o = ofertas.porId(ofertaId).orElseThrow();
            var aplicada = switch (decision) {
                case RespuestaEfa.Confirmada conf -> o.confirmar(conf.codigoSolicitud(), conf.condiciones());
                case RespuestaEfa.Denegada den -> o.denegar(den.codigoSolicitud(), den.motivo());
            };
            if (aplicada.esOk()) ofertas.guardar(o);
            return aplicada;
        });
    }
}
