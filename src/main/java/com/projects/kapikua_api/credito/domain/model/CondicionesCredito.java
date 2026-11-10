package com.projects.kapikua_api.credito.domain.model;

import com.projects.kapikua_api.shared.Dinero;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record CondicionesCredito(Dinero monto, int cuotas, BigDecimal tea, BigDecimal tcea, Dinero cuota) {
    public CondicionesCredito {
        Objects.requireNonNull(monto);
        Objects.requireNonNull(cuota);
        tea = tea.setScale(2, RoundingMode.HALF_EVEN);
        tcea = tcea.setScale(2, RoundingMode.HALF_EVEN);
        if (monto.esCero() || cuotas <= 0 || tea.signum() <= 0 || tcea.compareTo(tea) < 0 || cuota.esCero()) {
            throw new IllegalArgumentException("Condiciones de crédito inválidas");
        }
    }
}
