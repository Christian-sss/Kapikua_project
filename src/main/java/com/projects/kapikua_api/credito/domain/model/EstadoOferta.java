package com.projects.kapikua_api.credito.domain.model;

public enum EstadoOferta {
    PENDIENTE_EFA,
    CONFIRMADA,
    DENEGADA,
    ACEPTADA,
    VENCIDA;

    public boolean enCurso() {
        return this == PENDIENTE_EFA || this == CONFIRMADA;
    }
}
