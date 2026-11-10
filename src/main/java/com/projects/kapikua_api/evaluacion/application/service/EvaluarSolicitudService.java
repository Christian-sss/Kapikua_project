package com.projects.kapikua_api.evaluacion.application.service;

import com.projects.kapikua_api.evaluacion.application.port.in.EvaluarSolicitudUseCase;
import com.projects.kapikua_api.evaluacion.application.port.in.ResultadoEvaluacion;
import com.projects.kapikua_api.evaluacion.application.port.out.CentralDeRiesgos;
import com.projects.kapikua_api.evaluacion.application.port.out.CobrosCliente;
import com.projects.kapikua_api.evaluacion.application.port.out.EvaluacionRepositoryPort;
import com.projects.kapikua_api.evaluacion.application.port.out.HistorialRepositoryPort;
import com.projects.kapikua_api.evaluacion.application.port.out.IdentidadCliente;
import com.projects.kapikua_api.evaluacion.application.port.out.ParametrosPolitica;
import com.projects.kapikua_api.evaluacion.domain.error.ErrorEvaluacion;
import com.projects.kapikua_api.evaluacion.domain.model.Evaluacion;
import com.projects.kapikua_api.evaluacion.domain.model.HistorialCrediticio;
import com.projects.kapikua_api.evaluacion.domain.model.InformeCentral;
import com.projects.kapikua_api.evaluacion.domain.model.ParametrosCredito;
import com.projects.kapikua_api.evaluacion.domain.service.PoliticaCredito;
import com.projects.kapikua_api.evaluacion.domain.model.SolicitudCredito;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.puerto.PublicadorEventos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Service
public class EvaluarSolicitudService implements EvaluarSolicitudUseCase {
    private static final Logger log = LoggerFactory.getLogger(EvaluarSolicitudService.class);

    private final IdentidadCliente identidad;
    private final ParametrosPolitica parametros;
    private final HistorialRepositoryPort historiales;
    private final CobrosCliente cobros;
    private final CentralDeRiesgos central;
    private final EvaluacionRepositoryPort evaluaciones;
    private final PublicadorEventos publicador;
    private final TransactionTemplate transaccion;
    private final Clock clock;

    public EvaluarSolicitudService(IdentidadCliente identidad, ParametrosPolitica parametros,
                                   HistorialRepositoryPort historiales, CobrosCliente cobros, CentralDeRiesgos central,
                                   EvaluacionRepositoryPort evaluaciones, PublicadorEventos publicador,
                                   TransactionTemplate transaccion, Clock clock) {
        this.identidad = identidad;
        this.parametros = parametros;
        this.historiales = historiales;
        this.cobros = cobros;
        this.central = central;
        this.evaluaciones = evaluaciones;
        this.publicador = publicador;
        this.transaccion = transaccion;
        this.clock = clock;
    }

    @Override
    public Result<ResultadoEvaluacion, ErrorEvaluacion> evaluar(UsuarioId usuarioId, SolicitudCredito solicitud) {
        var inicio = System.nanoTime();
        if (!identidad.autorizoEvaluacionConBilletera(usuarioId)) {
            return Result.err(new ErrorEvaluacion.ConsentimientoFaltante());
        }
        var datos = identidad.datos(usuarioId);
        if (datos.isEmpty()) return Result.err(new ErrorEvaluacion.UsuarioNoEncontrado());

        var hoy = LocalDate.now(clock);
        var p = parametros.vigentes(hoy);
        if (p instanceof Result.Err<ParametrosCredito, ErrorEvaluacion> err) return Result.err(err.error());
        var politica = ((Result.Ok<ParametrosCredito, ErrorEvaluacion>) p).valor();

        var historial = historiales.porUsuario(usuarioId).orElseGet(() -> HistorialCrediticio.nuevo(usuarioId));
        var periodo = cobros.delPeriodo(usuarioId, hoy, politica.periodoCobrosSemanas());
        if (periodo.isEmpty()) return Result.err(new ErrorEvaluacion.UsuarioNoEncontrado());

        var elegible = PoliticaCredito.verificarElegibilidad(solicitud, periodo.get(), historial, politica);
        if (elegible instanceof Result.Err<Void, ErrorEvaluacion> err) return Result.err(err.error());

        var informe = central.consultar(datos.get().dni());
        if (informe instanceof Result.Err<InformeCentral, ErrorEvaluacion> err) return Result.err(err.error());

        var resultado = PoliticaCredito.evaluar(usuarioId, solicitud, periodo.get(),
                ((Result.Ok<InformeCentral, ErrorEvaluacion>) informe).valor(), historial, politica,
                OffsetDateTime.now(clock));

        return switch (resultado) {
            case Result.Err<Evaluacion, ErrorEvaluacion> err -> Result.err(err.error());
            case Result.Ok<Evaluacion, ErrorEvaluacion> ok -> {
                var guardada = transaccion.execute(estado -> {
                    var e = evaluaciones.registrar(ok.valor());
                    publicador.publicarTodos(e.eventos());
                    return e;
                });
                log.info("Evaluación {} del usuario {}: {} (score {}) en {} ms", guardada.id(), usuarioId.valor(),
                        guardada.recomendacion(), guardada.score(), (System.nanoTime() - inicio) / 1_000_000);
                yield Result.ok(ResultadoEvaluacion.de(guardada));
            }
        };
    }

    @Override
    public Result<Void, ErrorEvaluacion> evaluarAval(UsuarioId avalId) {
        var datos = identidad.datos(avalId);
        if (datos.isEmpty()) return Result.err(new ErrorEvaluacion.AvalIdentidadNoValidada());
        var informe = central.consultar(datos.get().dni());
        if (informe instanceof Result.Err<InformeCentral, ErrorEvaluacion> err) return Result.err(err.error());
        var historial = historiales.porUsuario(avalId).orElseGet(() -> HistorialCrediticio.nuevo(avalId));
        return PoliticaCredito.evaluarAval(datos.get().identidadValidada(),
                ((Result.Ok<InformeCentral, ErrorEvaluacion>) informe).valor(), historial);
    }
}
