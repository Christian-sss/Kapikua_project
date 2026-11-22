package com.projects.kapikua_api.credito.model;

import com.projects.kapikua_api.credito.error.ErrorCredito;
import com.projects.kapikua_api.shared.Result;

import java.time.OffsetDateTime;

final class AvalAceptado extends EstadoAval {
    AvalAceptado() {
        super("ACEPTADO");
    }

    @Override
    boolean estaAceptado() { return true; }

    @Override
    boolean avisar(Aval aval, OffsetDateTime ahora) {
        return aval.registrarAviso(ahora);
    }

    @Override
    Result<CambioEstado, ErrorCredito> activar(Aval aval, OffsetDateTime ahora) {
        return Result.ok(aval.aplicarActivacion(ahora));
    }

    @Override
    Result<CambioEstado, ErrorCredito> liberar(Aval aval, OffsetDateTime ahora) {
        return Result.ok(aval.aplicarLiberacion(ahora));
    }
}
