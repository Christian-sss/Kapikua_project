package com.projects.kapikua_api.shared.evento;

import com.projects.kapikua_api.shared.Dinero;

import java.time.OffsetDateTime;

/**
 * Evento de integración: Billetera registró una operación de dinero.
 * Cumplimiento lo usará después del commit para las reglas de alerta (RN-28).
 *
 * @param billeteraOrigenId  vacío en una recarga o un desembolso
 * @param billeteraDestinoId vacío en un pago de cuota
 */
public record TransaccionRegistrada(Long transaccionId,
                                    String tipo,
                                    Dinero monto,
                                    Long billeteraOrigenId,
                                    Long billeteraDestinoId,
                                    OffsetDateTime fecha) {}
