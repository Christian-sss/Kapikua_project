package com.projects.kapikua_api.billetera.dto;

import com.projects.kapikua_api.shared.Dinero;

public record Cargo(
        Long transaccionId,
        Dinero cobrado,
        Dinero deApartado,
        boolean nuevo
) {
    public static final Cargo NADA = new Cargo(null, Dinero.CERO, Dinero.CERO, false);
}
