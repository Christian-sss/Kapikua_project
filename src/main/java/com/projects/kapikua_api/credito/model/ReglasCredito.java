package com.projects.kapikua_api.credito.model;

import com.projects.kapikua_api.shared.Dinero;

import java.math.BigDecimal;
import java.time.Duration;

/** Parámetros de la política de crédito vigentes en una fecha (los lee ReglasCreditoService). */
public record ReglasCredito(int vigenciaOfertaDias, Duration plazoRespuestaAval, int maxAvales, Dinero cuotaMinima,
                            Dinero topePorOperacion, BigDecimal tasaMoratoriaMaxima, int diasAvisoAval,
                            int diasActivacionAval) {
}
