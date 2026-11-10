package com.projects.kapikua_api.evaluacion.domain.model;

import java.math.BigDecimal;

public record CondicionesNivel(int nivel, BigDecimal porcentajeCapacidad, int maxCuotas, BigDecimal tea,
                               int creditosParaSubir) {}
