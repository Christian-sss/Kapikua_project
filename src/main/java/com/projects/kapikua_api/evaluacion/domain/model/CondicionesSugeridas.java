package com.projects.kapikua_api.evaluacion.domain.model;

import com.projects.kapikua_api.shared.Dinero;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record CondicionesSugeridas(
        Dinero monto,
        int cuotas,
        BigDecimal tea,
        Dinero cuota,
        int porcentajeRetencion
) {
    private static final CondicionesSugeridas NINGUNA = new CondicionesSugeridas(Dinero.CERO, 0, BigDecimal.ZERO,
            Dinero.CERO, 0);

    public CondicionesSugeridas {
        Objects.requireNonNull(monto);
        Objects.requireNonNull(cuota);
        tea = tea.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static CondicionesSugeridas ninguna() {
        return NINGUNA;
    }

    public boolean hayOferta() {
        return cuotas > 0;
    }
}
