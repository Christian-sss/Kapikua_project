package com.projects.kapikua_api.evaluacion.domain.model;

import com.projects.kapikua_api.shared.Dinero;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Cobros del cliente en las semanas completas (lunes a domingo) antes de la solicitud: las últimas 12,
 * o las que haya desde la apertura (RN-12). Son cobros los pagos a negocio y las transferencias recibidas;
 * no cuentan recargas ni desembolsos (RN-08).
 *
 * @param semanas       semanas completas del periodo, incluidas las que no tuvieron cobros
 * @param diasAntiguedad días desde la apertura de la billetera
 */
public record CobrosDelPeriodo(int semanas, Dinero totalCobrado, int semanasConCobros, int cantidadCobros,
                               int pagadoresDistintos, long diasAntiguedad) {

    public CobrosDelPeriodo {
        Objects.requireNonNull(totalCobrado);
        if (semanas < 0 || semanasConCobros < 0 || semanasConCobros > semanas || cantidadCobros < 0
                || pagadoresDistintos < 0 || diasAntiguedad < 0) {
            throw new IllegalArgumentException("Resumen de cobros inconsistente");
        }
    }

    /** Total cobrado entre el número de semanas, incluidas las semanas sin cobros. Cero si no hay semanas. */
    public BigDecimal promedioSemanal() {
        if (semanas == 0) return BigDecimal.ZERO;
        return totalCobrado.valor().divide(BigDecimal.valueOf(semanas), 10, RoundingMode.HALF_EVEN);
    }
}
