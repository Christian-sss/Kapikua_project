package com.projects.kapikua_api.credito.domain.model;

public enum TipoPago {
    /** Pago de una cuota (manual, automático al vencimiento o del aval), con imputación (RN-23). */
    CUOTA,
    /** Pago adelantado total o parcial, sin penalidad, a capital (RN-27). */
    ANTICIPADO
}
