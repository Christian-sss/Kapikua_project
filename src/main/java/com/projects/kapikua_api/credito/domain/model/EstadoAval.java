package com.projects.kapikua_api.credito.domain.model;

public enum EstadoAval {
    PENDIENTE,
    ACEPTADO,
    RECHAZADO,
    ACTIVADO,
    LIBERADO;

    public boolean enCurso() {
        return this == PENDIENTE || this == ACEPTADO || this == ACTIVADO;
    }
}
