package com.projects.kapikua_api.billetera.service;

import com.projects.kapikua_api.identidad.client.CelularDisponible;
import org.springframework.stereotype.Component;

/** Le responde a Identidad, durante el registro, si un celular ya tiene billetera. */
@Component
class CelularDisponibleBilletera implements CelularDisponible {
    private final ConsultaBilleteraService billeteras;

    CelularDisponibleBilletera(ConsultaBilleteraService billeteras) {
        this.billeteras = billeteras;
    }

    @Override
    public boolean estaLibre(String celular) {
        return !billeteras.celularRegistrado(celular);
    }
}
