package com.projects.kapikua_api.evaluacion.domain.model;

import com.projects.kapikua_api.evaluacion.domain.event.EvaluacionEmitida;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.domain.EventoDominio;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Raíz de agregado inmutable (RN-19, RNF-10): una solicitud evaluada y su recomendación.
 * Solo se crea con {@link #noApto} o {@link #emitir}; una vez guardada no cambia (un trigger lo impide).
 *
 * @param score       null si una regla eliminatoria cortó la evaluación antes de calcularlo
 * @param condiciones null si la recomendación es NO_APTO
 * @param motivo      por qué NO_APTO o por qué se ajustó; null en un APTO
 */
public record Evaluacion(Long id,
                         UsuarioId usuarioId,
                         SolicitudCredito solicitud,
                         ClasificacionSbs clasificacionSbs,
                         Dinero deudaTotalSistema,
                         Integer score,
                         Recomendacion recomendacion,
                         CondicionesSugeridas condiciones,
                         boolean requiereAval,
                         String motivo,
                         List<FactorEvaluado> factores,
                         OffsetDateTime fecha) {

    public Evaluacion {
        Objects.requireNonNull(usuarioId);
        Objects.requireNonNull(solicitud);
        Objects.requireNonNull(recomendacion);
        Objects.requireNonNull(fecha);
        factores = List.copyOf(factores);
        if (recomendacion == Recomendacion.NO_APTO) {
            if (condiciones != null || requiereAval || motivo == null) {
                throw new IllegalArgumentException("Un NO_APTO no lleva condiciones ni aval, y sí un motivo");
            }
        } else {
            if (condiciones == null || score == null) {
                throw new IllegalArgumentException("Un APTO lleva score y condiciones");
            }
            if (recomendacion == Recomendacion.APTO_CON_AJUSTE && motivo == null) {
                throw new IllegalArgumentException("Un ajuste lleva motivo");
            }
        }
    }

    /** Solo la crea el servicio de dominio {@code PoliticaCredito} (regla que verificará ArchUnit). */
    public static Evaluacion noApto(UsuarioId usuarioId, SolicitudCredito solicitud, InformeCentral central,
                             Integer score, String motivo, List<FactorEvaluado> factores, OffsetDateTime ahora) {
        return new Evaluacion(null, usuarioId, solicitud, central.clasificacion(), central.deudaTotal(), score,
                Recomendacion.NO_APTO, null, false, motivo, factores, ahora);
    }

    /** Solo la crea el servicio de dominio {@code PoliticaCredito} (regla que verificará ArchUnit). */
    public static Evaluacion emitir(UsuarioId usuarioId, SolicitudCredito solicitud, InformeCentral central, int score,
                             Recomendacion recomendacion, CondicionesSugeridas condiciones, boolean requiereAval,
                             String motivo, List<FactorEvaluado> factores, OffsetDateTime ahora) {
        return new Evaluacion(null, usuarioId, solicitud, central.clasificacion(), central.deudaTotal(), score,
                recomendacion, condiciones, requiereAval, motivo, factores, ahora);
    }

    public Evaluacion conId(Long id) {
        return new Evaluacion(id, usuarioId, solicitud, clasificacionSbs, deudaTotalSistema, score, recomendacion,
                condiciones, requiereAval, motivo, factores, fecha);
    }

    public boolean esApta() {
        return recomendacion != Recomendacion.NO_APTO;
    }

    /** La evaluación es inmutable y su id lo asigna la base: el evento se deriva del registro ya guardado. */
    public List<EventoDominio> eventos() {
        if (id == null) throw new IllegalStateException("La evaluación aún no se ha guardado");
        return List.of(new EvaluacionEmitida(id, usuarioId, recomendacion, requiereAval));
    }
}
