package com.projects.kapikua_api.credito.domain.model;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.shared.Result;

import java.time.OffsetDateTime;

final class AvalPendiente extends EstadoAval {
    AvalPendiente() {
        super("PENDIENTE");
    }

    @Override
    boolean esPendiente() {
        return true;
    }

    @Override
    Result<CambioEstado, ErrorCredito> aceptar(
            Aval aval,
            int porcentajeRetencion,
            String dispositivo,
            OffsetDateTime ahora
    ) {
        return Result.ok(aval.aplicarAceptacion(porcentajeRetencion, dispositivo, ahora));
    }

    @Override
    Result<CambioEstado, ErrorCredito> rechazar(Aval aval, OffsetDateTime cuando) {
        return Result.ok(aval.aplicarRechazo(cuando));
    }
}
