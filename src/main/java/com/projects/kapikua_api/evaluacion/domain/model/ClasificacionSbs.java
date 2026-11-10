package com.projects.kapikua_api.evaluacion.domain.model;

public enum ClasificacionSbs {
    NORMAL,
    CPP,
    DEFICIENTE,
    DUDOSO,
    PERDIDA;

    public boolean esEliminatoria() {
        return this == DEFICIENTE || this == DUDOSO || this == PERDIDA;
    }
}
