package com.projects.kapikua_api.evaluacion.domain.model;

import java.math.BigDecimal;

public record FactorEvaluado(String nombre, String valor, BigDecimal puntaje, BigDecimal peso) {
    public BigDecimal aporte() {
        return puntaje.multiply(peso).divide(BigDecimal.valueOf(100));
    }
}
