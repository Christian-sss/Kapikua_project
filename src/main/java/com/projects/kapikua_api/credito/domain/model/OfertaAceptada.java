package com.projects.kapikua_api.credito.domain.model;

final class OfertaAceptada extends EstadoOferta {
    OfertaAceptada() {
        super("ACEPTADA");
    }

    @Override
    boolean estaAceptada() { return true; }

    @Override
    void validarReconstitucion(Oferta oferta) {
        oferta.validarConfirmacion();
    }
}
