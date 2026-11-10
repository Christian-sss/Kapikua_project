package com.projects.kapikua_api.credito.domain.model;

import com.projects.kapikua_api.shared.Dinero;

/** RN-23: cómo se aplicó un pago: primero al interés moratorio, luego al interés y al final al capital. */
public record Imputacion(Dinero moratorio, Dinero interes, Dinero capital) {

    public static final Imputacion NINGUNA = new Imputacion(Dinero.CERO, Dinero.CERO, Dinero.CERO);

    public static Imputacion soloCapital(Dinero capital) {
        return new Imputacion(Dinero.CERO, Dinero.CERO, capital);
    }

    public Dinero total() {
        return moratorio.sumar(interes).sumar(capital);
    }
}
