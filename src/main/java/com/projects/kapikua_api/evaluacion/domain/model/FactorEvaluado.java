package com.projects.kapikua_api.evaluacion.domain.model;

import java.math.BigDecimal;

/**
 * Un factor del score (RN-15): su valor observado, su puntaje de 0 a 100 y su peso.
 * Aporta puntaje × peso / 100 puntos. Se guardan todos con la evaluación para poder explicarla.
 */
public record FactorEvaluado(String nombre, String valor, BigDecimal puntaje, BigDecimal peso) {

    public BigDecimal aporte() {
        return puntaje.multiply(peso).divide(BigDecimal.valueOf(100));
    }
}
