package com.projects.kapikua_api.billetera.domain.model;

import com.projects.kapikua_api.shared.Dinero;

public record AcumuladoMensual(Dinero recargas, Dinero pagosYTransferencias) {
    public static final AcumuladoMensual VACIO = new AcumuladoMensual(Dinero.CERO, Dinero.CERO);
}
