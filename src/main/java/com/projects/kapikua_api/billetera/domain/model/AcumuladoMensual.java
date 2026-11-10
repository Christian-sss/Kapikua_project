package com.projects.kapikua_api.billetera.domain.model;

import com.projects.kapikua_api.shared.Dinero;

/** Lo que el titular recargó y lo que pagó o transfirió en el mes calendario (RN-03, RN-04). */
public record AcumuladoMensual(Dinero recargas, Dinero pagosYTransferencias) {

    public static final AcumuladoMensual VACIO = new AcumuladoMensual(Dinero.CERO, Dinero.CERO);
}
