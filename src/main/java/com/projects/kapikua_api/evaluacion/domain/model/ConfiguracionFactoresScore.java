package com.projects.kapikua_api.evaluacion.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record ConfiguracionFactoresScore(
        PonderacionesScore ponderaciones,
        ReferenciasScore referencias,
        PuntajesClasificacionSbs puntajesSbs) {


    public ConfiguracionFactoresScore {
        Objects.requireNonNull(ponderaciones);
        Objects.requireNonNull(referencias);
        Objects.requireNonNull(puntajesSbs);
    }

    public record PonderacionesScore(BigDecimal regularidad, BigDecimal holgura, BigDecimal historial,
                                    BigDecimal pagadores, BigDecimal antiguedad, BigDecimal clasificacion) {}

    public record ReferenciasScore(int refPagadores, int refAntiguedadDias, BigDecimal historialNuevo,
                                  BigDecimal historialMenosPorDia) {
        public ReferenciasScore {
            if (refPagadores <= 0 || refAntiguedadDias <= 0) {
                throw new IllegalArgumentException("Las referencias de pagadores y antigüedad deben ser positivas");
            }
        }
    }

    public record PuntajesClasificacionSbs(BigDecimal normal, BigDecimal sinHistorial, BigDecimal cpp) {}
}
