package com.projects.kapikua_api.evaluacion.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record ReglasScore(ReglasRecomendacion recomendacion, ConfiguracionFactoresScore factores) {
    public ReglasScore {
        Objects.requireNonNull(recomendacion);
        Objects.requireNonNull(factores);
    }

    public record ReglasRecomendacion(int scoreApto, int scoreAjuste, BigDecimal porcentajeAjuste) {
        public ReglasRecomendacion {
            Objects.requireNonNull(porcentajeAjuste);
            if (scoreAjuste < 0 || scoreAjuste > scoreApto) {
                throw new IllegalArgumentException("Umbrales de score fuera de orden");
            }
        }
    }
}
