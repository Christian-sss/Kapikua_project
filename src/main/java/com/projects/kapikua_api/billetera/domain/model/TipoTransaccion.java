package com.projects.kapikua_api.billetera.domain.model;

public enum TipoTransaccion {
    RECARGA,
    TRANSFERENCIA,
    PAGO_NEGOCIO,
    DESEMBOLSO,
    PAGO_CUOTA,
    PAGO_ANTICIPADO;

    public boolean cuentaComoPagoOTransferencia() {
        return this == TRANSFERENCIA || this == PAGO_NEGOCIO;
    }
}
