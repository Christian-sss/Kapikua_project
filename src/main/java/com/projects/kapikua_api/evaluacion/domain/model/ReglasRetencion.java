package com.projects.kapikua_api.evaluacion.domain.model;

import com.projects.kapikua_api.shared.Dinero;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record ReglasRetencion(
        int retencionMin,
        int retencionMax,
        int retencionDefecto
) {
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    public ReglasRetencion {
        if (retencionMin > retencionDefecto || retencionDefecto > retencionMax) {
            throw new IllegalArgumentException("La retención por defecto debe estar entre el mínimo y el máximo");
        }
    }

    public int porcentaje(Dinero cuota, BigDecimal promedioSemanal) {
        if (promedioSemanal.signum() == 0) {
            return retencionDefecto;
        }

        return cuota.valor()
                .multiply(CIEN)
                .divide(promedioSemanal, 0, RoundingMode.CEILING)
                .max(BigDecimal.valueOf(retencionMin))
                .min(BigDecimal.valueOf(retencionMax))
                .intValueExact();
    }
}
