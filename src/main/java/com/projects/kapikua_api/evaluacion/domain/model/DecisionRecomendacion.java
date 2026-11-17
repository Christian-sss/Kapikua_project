package com.projects.kapikua_api.evaluacion.domain.model;

import java.util.Objects;

public record DecisionRecomendacion(
        String recomendacion,
        CondicionesSugeridas condiciones,
        boolean requiereAval,
        String motivo

) {
    public DecisionRecomendacion {
        Objects.requireNonNull(recomendacion);
        Objects.requireNonNull(condiciones);
        if (!condiciones.hayOferta() && (requiereAval || motivo == null)) {
            throw new IllegalArgumentException("Una evaluación sin oferta lleva motivo y no requiere aval");
        }
        motivo = Objects.requireNonNullElse(motivo, "");
    }
}
