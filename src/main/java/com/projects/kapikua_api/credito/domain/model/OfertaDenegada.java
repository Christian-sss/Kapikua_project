package com.projects.kapikua_api.credito.domain.model;

import java.util.Objects;

final class OfertaDenegada extends EstadoOferta {
    OfertaDenegada() {
        super("DENEGADA");
    }

    @Override
    void validarReconstitucion(Oferta oferta) {
        Objects.requireNonNull(oferta.getMotivoDenegacion(), "La oferta denegada requiere un motivo");
    }
}
