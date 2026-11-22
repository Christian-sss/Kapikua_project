package com.projects.kapikua_api.credito.model;

import com.projects.kapikua_api.credito.error.ErrorCredito;
import com.projects.kapikua_api.shared.Result;

import java.time.OffsetDateTime;
import java.time.ZoneId;

final class OfertaPendienteEfa extends EstadoOferta {
    OfertaPendienteEfa() {
        super("PENDIENTE_EFA");
    }

    @Override
    boolean esperaDecisionEfa() { return true; }

    @Override
    Result<Void, ErrorCredito> verificarGestionAval() { return Result.ok(); }

    @Override
    Result<CambioEstado, ErrorCredito> confirmar(Oferta oferta, String codigo, CondicionesCredito condiciones,
                                                   OffsetDateTime ahora) {
        return oferta.confirmarCon(codigo, condiciones, ahora);
    }

    @Override
    Result<CambioEstado, ErrorCredito> denegar(Oferta oferta, String codigo, String motivo, OffsetDateTime ahora) {
        return Result.ok(oferta.denegarCon(codigo, motivo, ahora));
    }

    @Override
    Result<CambioEstado, ErrorCredito> vencer(Oferta oferta, OffsetDateTime ahora, OffsetDateTime finVigencia) {
        return oferta.vencerSiCorresponde(ahora, finVigencia);
    }

    @Override
    ErrorCredito errorAlAceptar(Oferta oferta, OffsetDateTime ahora, ZoneId zona) {
        return ahora.isBefore(oferta.finVigencia(zona)) ? errorEstado() : new ErrorCredito.OfertaNoVigente();
    }
}
