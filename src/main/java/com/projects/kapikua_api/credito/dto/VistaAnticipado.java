package com.projects.kapikua_api.credito.dto;

import com.projects.kapikua_api.shared.Dinero;

/** Lo que falta pagar, antes de elegir la modalidad del pago anticipado. */
public record VistaAnticipado(Dinero capitalPendiente, int cuotasPendientes, boolean hayVencidas, String referencia) {
}
