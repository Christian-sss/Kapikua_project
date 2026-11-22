package com.projects.kapikua_api.credito.dto;

import com.projects.kapikua_api.shared.Dinero;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Lo que el aval revisa antes de aceptar, con el código de un solo uso. */
public record InvitacionAval(Long ofertaId, String deudor, Dinero monto, int cuotas, Dinero cuota, BigDecimal tea,
                             int porcentajeRetencion, OffsetDateTime vence, String codigoMostrado) {
}
