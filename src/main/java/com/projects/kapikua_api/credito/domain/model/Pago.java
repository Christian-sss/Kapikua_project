package com.projects.kapikua_api.credito.domain.model;

import com.projects.kapikua_api.shared.Dinero;

import java.time.OffsetDateTime;

/**
 * Dinero que sale hacia la entidad aliada para el préstamo (RN-23, RN-27, RN-32). Inmutable: solo se inserta.
 * Un pago de cuota lleva la cuota y su imputación; un anticipado va a capital y no lleva cuota.
 *
 * @param transaccionId la transacción PAGO_CUOTA o PAGO_ANTICIPADO de la billetera
 */
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
