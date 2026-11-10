package com.projects.kapikua_api.credito.domain.model;

/** Valores del CHECK chk_oferta_estado del script v6. */
public enum EstadoOferta {
    PENDIENTE_EFA,
    CONFIRMADA,
    DENEGADA,
    ACEPTADA,
    VENCIDA;

    /** La oferta sigue viva: se puede designar aval, confirmar o aceptar. */
    public boolean enCurso() {
        return this == PENDIENTE_EFA || this == CONFIRMADA;
    }
}
