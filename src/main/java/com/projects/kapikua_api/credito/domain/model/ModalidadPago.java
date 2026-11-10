package com.projects.kapikua_api.credito.domain.model;

/** RN-27: qué hacer con un pago anticipado. */
public enum ModalidadPago {
    /** Cancela todo el capital pendiente. */
    TOTAL,
    /** Mismo número de cuotas, cada una más baja. */
    REDUCIR_CUOTA,
    /** Misma cuota, menos semanas. */
    REDUCIR_PLAZO
}
