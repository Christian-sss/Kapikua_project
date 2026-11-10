package com.projects.kapikua_api.evaluacion.domain.model;

import com.projects.kapikua_api.shared.Dinero;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record CondicionesSugeridas(Dinero monto, int cuotas, BigDecimal tea, Dinero cuota, int porcentajeRetencion) {
    public CondicionesSugeridas {
        tea = tea.setScale(2, RoundingMode.HALF_EVEN);
    }
}
