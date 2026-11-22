package com.projects.kapikua_api.credito.model;

import com.projects.kapikua_api.credito.error.ErrorCredito;

import java.time.OffsetDateTime;
import java.time.ZoneId;

final class OfertaVencida extends EstadoOferta {
    OfertaVencida() {
        super("VENCIDA");
    }

    @Override
    ErrorCredito errorAlAceptar(Oferta oferta, OffsetDateTime ahora, ZoneId zona) {
        return new ErrorCredito.OfertaNoVigente();
    }
}
