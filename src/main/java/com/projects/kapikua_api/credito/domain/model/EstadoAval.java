package com.projects.kapikua_api.credito.domain.model;

/** Valores del CHECK chk_aval_estado del script v6. */
public enum EstadoAval {
    PENDIENTE,
    ACEPTADO,
    RECHAZADO,
    ACTIVADO,
    LIBERADO;

    /** Ocupa el lugar del aval en la oferta (uq_aval_en_curso_por_oferta). */
    public boolean enCurso() {
        return this == PENDIENTE || this == ACEPTADO || this == ACTIVADO;
    }
}
