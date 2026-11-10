package com.projects.kapikua_api.evaluacion.domain.model;

import java.math.BigDecimal;

/**
 * Lo que permite un nivel de la escalera (RN-13, RN-14, RN-26). Viene de los parámetros, no del código.
 *
 * @param porcentajeCapacidad qué parte de la capacidad de pago se confía en este nivel
 * @param creditosParaSubir   créditos pagados a tiempo para subir al siguiente nivel (0 en el último)
 */
public record CondicionesNivel(int nivel, BigDecimal porcentajeCapacidad, int maxCuotas, BigDecimal tea,
                               int creditosParaSubir) {}
