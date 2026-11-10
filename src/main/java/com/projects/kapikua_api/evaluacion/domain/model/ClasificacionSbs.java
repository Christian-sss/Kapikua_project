package com.projects.kapikua_api.evaluacion.domain.model;

/**
 * Clasificación del deudor (Res. SBS 11356-2008): Normal hasta 8 días de atraso, CPP de 9 a 30,
 * Deficiente de 31 a 60, Dudoso de 61 a 120 y Pérdida más de 120. Null significa sin historial.
 */
public enum ClasificacionSbs {
    NORMAL,
    CPP,
    DEFICIENTE,
    DUDOSO,
    PERDIDA;

    /** RN-10. */
    public boolean esEliminatoria() {
        return this == DEFICIENTE || this == DUDOSO || this == PERDIDA;
    }
}
