package com.projects.kapikua_api.credito.dto;

import com.projects.kapikua_api.shared.Dinero;

import java.time.OffsetDateTime;

/** Un préstamo que el usuario respalda como aval. */
public record Garantia(Long prestamoId, String deudor, String estadoAval, String estadoPrestamo, Dinero monto,
                       int cuotasVencidas, Dinero vencido, OffsetDateTime avisadoEl) {
}
