package com.projects.kapikua_api.evaluacion.domain.model;

import com.projects.kapikua_api.evaluacion.domain.event.EvaluacionEmitida;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.domain.EventoDominio;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

public record Evaluacion(
        Long id,
        UsuarioId usuarioId,
        SolicitudCredito solicitud,
        ClasificacionSbs clasificacionSbs,
        Dinero deudaTotalSistema,
        DetalleScore detalleScore,
        DecisionRecomendacion decision,
        OffsetDateTime fecha) {

    public Evaluacion {
        Objects.requireNonNull(usuarioId);
        Objects.requireNonNull(solicitud);
        Objects.requireNonNull(detalleScore);
        Objects.requireNonNull(decision);
        Objects.requireNonNull(fecha);
        if (decision.condiciones().hayOferta() && detalleScore.puntaje() == null) {
            throw new IllegalArgumentException("Una recomendación apta lleva score");
        }
    }

    public record DetalleScore(Integer puntaje, List<FactorEvaluado> factores) {
        public DetalleScore {
            factores = List.copyOf(factores);
        }
    }

    public static Evaluacion apto(ContextoEvaluacion contexto, CondicionesSugeridas condiciones) {
        return conOferta(contexto, "APTO", condiciones, "");
    }

    public static Evaluacion aptoConAjuste(ContextoEvaluacion contexto, CondicionesSugeridas condiciones, String motivo) {
        return conOferta(contexto, "APTO_CON_AJUSTE", condiciones, Objects.requireNonNull(motivo));
    }

    public static Evaluacion noApto(ContextoEvaluacion contexto, String motivo) {
        return desde(contexto, new DecisionRecomendacion("NO_APTO", CondicionesSugeridas.ninguna(), false, motivo));
    }

    private static Evaluacion conOferta(ContextoEvaluacion contexto, String recomendacion,
                                       CondicionesSugeridas condiciones, String motivo) {
        if (!condiciones.hayOferta()) {
            throw new IllegalArgumentException("Una recomendación apta lleva condiciones");
        }
        return desde(contexto, new DecisionRecomendacion(recomendacion, condiciones, contexto.requiereAval(), motivo));
    }

    private static Evaluacion desde(ContextoEvaluacion contexto, DecisionRecomendacion decision) {
        return new Evaluacion(null, contexto.usuarioId(), contexto.solicitud(), contexto.central().clasificacion(),
                contexto.central().deudaTotal(), contexto.detalleScore(), decision, contexto.fecha());
    }

    public Evaluacion conId(Long id) {
        return new Evaluacion(id, usuarioId, solicitud, clasificacionSbs, deudaTotalSistema, detalleScore, decision, fecha);
    }

    public Integer score() {
        return detalleScore.puntaje();
    }

    public List<FactorEvaluado> factores() {
        return detalleScore.factores();
    }

    public String recomendacion() {
        return decision.recomendacion();
    }

    public CondicionesSugeridas condiciones() {
        return decision.condiciones();
    }

    public boolean requiereAval() {
        return decision.requiereAval();
    }

    public String motivo() {
        return decision.motivo();
    }

    public boolean esApta() {
        return condiciones().hayOferta();
    }

    public List<EventoDominio> eventos() {
        if (id == null) throw new IllegalStateException("La evaluación aún no se ha guardado");
        return List.of(new EvaluacionEmitida(id, usuarioId, recomendacion(), requiereAval()));
    }
}
