package com.projects.kapikua_api.credito.domain.model;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.shared.Result;

import java.time.OffsetDateTime;

final class AvalActivado extends EstadoAval {
    AvalActivado() {
        super("ACTIVADO");
    }

    @Override
    boolean estaActivado() { return true; }

    @Override
    boolean avisar(Aval aval, OffsetDateTime ahora) {
        return aval.registrarAviso(ahora);
    }

    @Override
    Result<CambioEstado, ErrorCredito> liberar(Aval aval, OffsetDateTime ahora) {
        return Result.ok(aval.aplicarLiberacion(ahora));
    }
}
