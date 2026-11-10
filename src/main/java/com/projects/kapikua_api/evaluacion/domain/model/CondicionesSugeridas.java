package com.projects.kapikua_api.evaluacion.domain.model;

import com.projects.kapikua_api.shared.Dinero;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Condiciones que Kapikua sugiere a la entidad aliada (solo con APTO o APTO_CON_AJUSTE).
 *
 * @param tea                 en porcentaje, nunca mayor que la tasa máxima del BCRP (RN-16)
 * @param cuota               cuota semanal del sistema francés, nunca menor que la mínima (RN-17)
 * @param porcentajeRetencion de las ventas, entre 5 y 15 (RN-22)
 */
public record CondicionesSugeridas(Dinero monto, int cuotas, BigDecimal tea, Dinero cuota, int porcentajeRetencion) {

    /** La TEA se expresa con 2 decimales, como la guarda la base (NUMERIC(6,2)). */
    public CondicionesSugeridas {
        tea = tea.setScale(2, RoundingMode.HALF_EVEN);
    }
}
