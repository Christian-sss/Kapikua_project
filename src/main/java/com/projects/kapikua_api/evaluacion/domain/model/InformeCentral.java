package com.projects.kapikua_api.evaluacion.domain.model;

import com.projects.kapikua_api.shared.Dinero;

import java.util.Objects;

/** Respuesta de la Central de Riesgos de la SBS. {@code clasificacion} null = sin historial. */
public record InformeCentral(ClasificacionSbs clasificacion, Dinero deudaTotal) {

    public InformeCentral {
        Objects.requireNonNull(deudaTotal);
    }
}
