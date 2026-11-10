package com.projects.kapikua_api.credito.domain.model;

import com.projects.kapikua_api.shared.Dinero;

import java.time.OffsetDateTime;

/**
 * RN-22: lo que se apartó de una venta para una cuota. Una venta se retiene una sola vez
 * (credito.retencion.transaccion_id es único). Inmutable: solo se inserta.
 *
 * @param transaccionVentaId el cobro del que salió (billetera.transaccion)
 */
public record Retencion(Long cuotaId, Long transaccionVentaId, Origen origen, Dinero monto, OffsetDateTime fecha) {}
