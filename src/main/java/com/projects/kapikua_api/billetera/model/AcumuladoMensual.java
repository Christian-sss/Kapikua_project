package com.projects.kapikua_api.billetera.model;

import com.projects.kapikua_api.shared.Dinero;

public record AcumuladoMensual(
        Dinero pagosYTransferencias
) {
    public static final AcumuladoMensual VACIO = new AcumuladoMensual(Dinero.CERO);
}
