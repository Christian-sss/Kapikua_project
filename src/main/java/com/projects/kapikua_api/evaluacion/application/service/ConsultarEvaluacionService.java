package com.projects.kapikua_api.evaluacion.application.service;

import com.projects.kapikua_api.evaluacion.application.port.in.ConsultarEvaluacionUseCase;
import com.projects.kapikua_api.evaluacion.application.port.in.ResultadoEvaluacion;
import com.projects.kapikua_api.evaluacion.application.port.out.CobrosCliente;
import com.projects.kapikua_api.evaluacion.application.port.out.EvaluacionRepositoryPort;
import com.projects.kapikua_api.evaluacion.application.port.out.HistorialRepositoryPort;
import com.projects.kapikua_api.evaluacion.application.port.out.ParametrosPolitica;
import com.projects.kapikua_api.evaluacion.domain.error.ErrorEvaluacion;
import com.projects.kapikua_api.evaluacion.domain.model.HistorialCrediticio;
import com.projects.kapikua_api.evaluacion.domain.model.ParametrosCredito;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

@Service
public class ConsultarEvaluacionService implements ConsultarEvaluacionUseCase {

    private final EvaluacionRepositoryPort evaluaciones;
    private final HistorialRepositoryPort historiales;
    private final ParametrosPolitica parametros;
    private final CobrosCliente cobros;
    private final Clock clock;

    public ConsultarEvaluacionService(EvaluacionRepositoryPort evaluaciones, HistorialRepositoryPort historiales,
                                      ParametrosPolitica parametros, CobrosCliente cobros, Clock clock) {
        this.evaluaciones = evaluaciones;
        this.historiales = historiales;
        this.parametros = parametros;
        this.cobros = cobros;
        this.clock = clock;
    }

    @Override
    public Optional<ResultadoEvaluacion> porId(Long evaluacionId) {
        return evaluaciones.porId(evaluacionId).map(ResultadoEvaluacion::de);
    }

    @Override
    public Result<NivelCliente, ErrorEvaluacion> nivel(UsuarioId usuarioId) {
        var hoy = LocalDate.now(clock);
        var resultado = parametros.vigentes(hoy);
        if (resultado instanceof Result.Err<ParametrosCredito, ErrorEvaluacion> err) return Result.err(err.error());
        var politica = ((Result.Ok<ParametrosCredito, ErrorEvaluacion>) resultado).valor();
        var usoMinimo = politica.usoMinimo();
        var limites = politica.limitesCredito();
        var reglasNivel = politica.reglasNivel();

        var h = historiales.porUsuario(usuarioId).orElseGet(() -> HistorialCrediticio.nuevo(usuarioId));
        var actual = reglasNivel.nivel(h.getNivel());
        var periodo = cobros.delPeriodo(usuarioId, hoy, usoMinimo.periodoCobrosSemanas());
        if (periodo.isEmpty()) return Result.err(new ErrorEvaluacion.UsuarioNoEncontrado());
        var c = periodo.get();

        var uso = new UsoMinimo(c.diasAntiguedad(), usoMinimo.usoMinDias(), c.semanasConCobros(),
                usoMinimo.usoMinSemanasConCobro(), c.cantidadCobros(), usoMinimo.usoMinCobros(),
                Dinero.de(c.promedioSemanal()));
        Siguiente siguiente = null;
        var nivelMaximo = reglasNivel.nivelMaximo();
        if (h.getNivel() < nivelMaximo) {
            var s = reglasNivel.nivel(h.getNivel() + 1);
            siguiente = new Siguiente(s.nivel(), s.porcentajeCapacidad(), s.maxCuotas(),
                    s.tea().min(limites.tasaMaxima()), s.nivel() <= reglasNivel.nivelMaxConAval());
        }
        var escalones = new ArrayList<Escalon>();
        for (int k = 1; k <= nivelMaximo; k++) {
            var cn = reglasNivel.nivel(k);
            int paraLlegar = k == 1 ? 0 : reglasNivel.nivel(k - 1).creditosParaSubir();
            escalones.add(new Escalon(k, cn.porcentajeCapacidad(), cn.maxCuotas(), cn.tea().min(limites.tasaMaxima()),
                    k <= reglasNivel.nivelMaxConAval(), paraLlegar));
        }
        return Result.ok(new NivelCliente(h.getNivel(), nivelMaximo, actual.porcentajeCapacidad(),
                limites.cuotasMinimas(), actual.maxCuotas(), actual.tea().min(limites.tasaMaxima()),
                h.getCreditosPagadosEnNivel(), actual.creditosParaSubir(), h.getNivel() <= reglasNivel.nivelMaxConAval(),
                h.isBloqueadoInterno(), uso, siguiente, escalones));
    }
}
