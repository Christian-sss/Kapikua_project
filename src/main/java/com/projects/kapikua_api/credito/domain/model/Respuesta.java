package com.projects.kapikua_api.credito.domain.model;

import java.time.OffsetDateTime;

public record Respuesta(
        OffsetDateTime fechaRespuesta,
        String dispositivoAceptacion,
        Integer porcentajeRetencionAutorizado
) {
    public static final Respuesta NINGUNA = new Respuesta(null, null, null);
}
