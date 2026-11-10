package com.projects.kapikua_api.billetera.domain.model;

/** Valores del CHECK chk_transaccion_tipo del script v6. */
public enum TipoTransaccion {
    RECARGA,
    TRANSFERENCIA,
    PAGO_NEGOCIO,
    DESEMBOLSO,
    PAGO_CUOTA,
    PAGO_ANTICIPADO;

    /** Cuentan para el tope mensual de pagos y transferencias del que envía (RN-04). */
    public boolean cuentaComoPagoOTransferencia() {
        return this == TRANSFERENCIA || this == PAGO_NEGOCIO;
    }
}
