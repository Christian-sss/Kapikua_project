package com.projects.kapikua_api.credito.domain.model;

import com.projects.kapikua_api.shared.Dinero;

import java.time.OffsetDateTime;

public record Pago(Long cuotaId, Long transaccionId, TipoPago tipo, Origen origen, boolean automatico,
                   ModalidadPago modalidad, Dinero monto, Imputacion imputacion, OffsetDateTime fecha) {
    public Pago {
        if (!imputacion.total().equals(monto)) throw new IllegalArgumentException("La imputación no suma el monto");
        if (tipo == TipoPago.CUOTA && (cuotaId == null || modalidad != null)) {
            throw new IllegalArgumentException("Un pago de cuota lleva cuota y no lleva modalidad");
        }
        if (tipo == TipoPago.ANTICIPADO && (cuotaId != null || origen != Origen.DEUDOR || automatico || modalidad == null)) {
            throw new IllegalArgumentException("Un pago anticipado es del deudor, manual, sin cuota y con modalidad");
        }
    }
}
