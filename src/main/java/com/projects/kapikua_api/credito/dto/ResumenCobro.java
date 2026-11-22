package com.projects.kapikua_api.credito.dto;

import com.projects.kapikua_api.shared.Dinero;

/** Resultado del cobro diario programado. */
public record ResumenCobro(int ofertasActualizadas, int prestamosRevisados, int cuotasCobradas, Dinero totalCobrado,
                           int prestamosEnMora, int avisosAval, int avalesActivados, int prestamosPagados,
                           int errores) {
}
