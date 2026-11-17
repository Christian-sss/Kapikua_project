package com.projects.kapikua_api.credito.domain.model;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.shared.Result;

import java.time.OffsetDateTime;
import java.time.ZoneId;

final class OfertaConfirmada extends EstadoOferta {
    OfertaConfirmada() {
        super("CONFIRMADA");
    }

    @Override
    boolean estaConfirmada() { return true; }

    @Override
    void validarReconstitucion(Oferta oferta) {
        oferta.validarConfirmacion();
    }

    @Override
    Result<Void, ErrorCredito> verificarAceptable(Oferta oferta, boolean requiereAval, OffsetDateTime ahora, ZoneId zona) {
        if (!ahora.isBefore(oferta.finVigencia(zona))) return Result.err(new ErrorCredito.OfertaNoVigente());
        if (requiereAval && oferta.avalAceptado().isEmpty()) return Result.err(new ErrorCredito.AvalRequerido());
        return Result.ok();
    }

    @Override
    Result<CambioEstado, ErrorCredito> aceptar(Oferta oferta, boolean requiereAval, OffsetDateTime ahora, ZoneId zona) {
        return verificarAceptable(oferta, requiereAval, ahora, zona)
                .map(ignorado -> oferta.cambiarEstado(ACEPTADA, "Aceptada por el titular", ahora));
    }

    @Override
    Result<CambioEstado, ErrorCredito> vencer(Oferta oferta, OffsetDateTime ahora, OffsetDateTime finVigencia) {
        return oferta.vencerSiCorresponde(ahora, finVigencia);
    }
}
