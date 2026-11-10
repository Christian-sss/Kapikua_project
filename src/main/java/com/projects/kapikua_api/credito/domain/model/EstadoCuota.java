package com.projects.kapikua_api.credito.domain.model;

public enum EstadoCuota {
    PENDIENTE,
    VENCIDA,
    PAGADA,
    /** Eliminada por un pago anticipado que reduce el plazo. */
    ANULADA
}
