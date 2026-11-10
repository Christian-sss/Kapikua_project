package com.projects.kapikua_api.evaluacion.domain.model;

import com.projects.kapikua_api.shared.Dinero;

import java.util.Objects;

/** Lo que pide el cliente: monto, número de cuotas semanales y destino (RF-CR-02). */
public record SolicitudCredito(Dinero monto, int cuotas, Destino destino) {

    public SolicitudCredito {
        Objects.requireNonNull(monto);
        Objects.requireNonNull(destino);
    }
}
