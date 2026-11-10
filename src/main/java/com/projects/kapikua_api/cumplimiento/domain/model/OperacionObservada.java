package com.projects.kapikua_api.cumplimiento.domain.model;

import com.projects.kapikua_api.shared.Dinero;

import java.time.OffsetDateTime;

public record OperacionObservada(Long transaccionId, String tipo, Dinero monto, OffsetDateTime fecha) {
    public boolean esEnvio() {
        return "TRANSFERENCIA".equals(tipo) || "PAGO_NEGOCIO".equals(tipo);
    }
}
