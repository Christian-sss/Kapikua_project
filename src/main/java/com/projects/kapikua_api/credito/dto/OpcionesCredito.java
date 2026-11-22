package com.projects.kapikua_api.credito.dto;

import com.projects.kapikua_api.shared.Dinero;

import java.math.BigDecimal;

/** Los límites que el nivel del cliente le permite pedir. */
public record OpcionesCredito(int nivel, int cuotasMinimas, int cuotasMaximas, BigDecimal tea, Dinero montoMaximo) {
}
