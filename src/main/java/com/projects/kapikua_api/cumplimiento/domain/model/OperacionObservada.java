package com.projects.kapikua_api.cumplimiento.domain.model;

import com.projects.kapikua_api.shared.Dinero;

import java.time.OffsetDateTime;

/** Una operación del titular de la billetera, tal como la ve Cumplimiento (recarga o envío). */
public record OperacionObservada(Long transaccionId, String tipo, Dinero monto, OffsetDateTime fecha) {

    /** Transferencias y pagos a negocio cuentan para TRANSFERENCIAS_SEGUIDAS; las recargas no. */
    public boolean esEnvio() {
        return "TRANSFERENCIA".equals(tipo) || "PAGO_NEGOCIO".equals(tipo);
    }
}
