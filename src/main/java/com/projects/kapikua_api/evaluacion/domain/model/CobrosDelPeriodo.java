package com.projects.kapikua_api.evaluacion.domain.model;

import com.projects.kapikua_api.shared.Dinero;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record CobrosDelPeriodo(int semanas, Dinero totalCobrado, int semanasConCobros, int cantidadCobros,
                               int pagadoresDistintos, long diasAntiguedad) {
    public CobrosDelPeriodo {
        Objects.requireNonNull(totalCobrado);
        if (semanas < 0 || semanasConCobros < 0 || semanasConCobros > semanas || cantidadCobros < 0
                || pagadoresDistintos < 0 || diasAntiguedad < 0) {
            throw new IllegalArgumentException("Resumen de cobros inconsistente");
        }
    }

    public BigDecimal promedioSemanal() {
        if (semanas == 0) return BigDecimal.ZERO;
        return totalCobrado.valor().divide(BigDecimal.valueOf(semanas), 10, RoundingMode.HALF_EVEN);
    }
}
