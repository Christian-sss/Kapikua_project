package com.projects.kapikua_api.credito.model;

import com.projects.kapikua_api.shared.Dinero;

import java.time.OffsetDateTime;
import java.util.Objects;

public record Pago(Long cuotaId, Long transaccionId, TipoPago tipo, Origen origen, boolean automatico,
                   ModalidadPago modalidad, Dinero monto, Imputacion imputacion, OffsetDateTime fecha) {
    public Pago {
        Objects.requireNonNull(modalidad);
        if (!imputacion.total().equals(monto)) throw new IllegalArgumentException("La imputación no suma el monto");
        if (tipo == TipoPago.CUOTA && (cuotaId == null || modalidad.esAnticipada())) {
            throw new IllegalArgumentException("Un pago de cuota lleva cuota y modalidad regular");
        }
        if (tipo == TipoPago.ANTICIPADO
                && (cuotaId != null || origen != Origen.DEUDOR || automatico || !modalidad.esAnticipada())) {
            throw new IllegalArgumentException("Un pago anticipado es manual, del deudor y con una modalidad anticipada");
        }
    }
}
