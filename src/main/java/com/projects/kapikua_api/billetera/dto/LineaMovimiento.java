package com.projects.kapikua_api.billetera.dto;

import com.projects.kapikua_api.billetera.model.TipoTransaccion;
import com.projects.kapikua_api.shared.Dinero;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record LineaMovimiento(Long transaccionId, TipoTransaccion tipo, String descripcion, BigDecimal monto,
                              BigDecimal saldoPosterior, OffsetDateTime fecha) {
    public boolean esEntrada() {
        return monto.signum() > 0;
    }

    public Dinero montoAbsoluto() {
        return Dinero.de(monto.abs());
    }
}
