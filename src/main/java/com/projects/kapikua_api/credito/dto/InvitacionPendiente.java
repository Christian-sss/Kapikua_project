package com.projects.kapikua_api.credito.dto;

import com.projects.kapikua_api.shared.Dinero;

import java.time.OffsetDateTime;

/** Invitación a ser aval que el usuario todavía no responde. */
public record InvitacionPendiente(Long ofertaId, String deudor, Dinero monto, OffsetDateTime vence) {
}
