package com.projects.kapikua_api.evaluacion.domain.model;

import com.projects.kapikua_api.shared.Dinero;

import java.math.BigDecimal;

public record LimitesCredito(
        BigDecimal porcentajeCapacidadPago,
        Dinero deudaMaxima,
        Dinero topePorOperacion,
        BigDecimal tasaMaxima,
        Dinero cuotaMinima,
        int cuotasMinimas
) {
}
