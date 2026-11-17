package com.projects.kapikua_api.evaluacion.domain.event;

import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.domain.EventoDominio;

public record EvaluacionEmitida(
        Long evaluacionId,
        UsuarioId usuarioId,
        String recomendacion,
        boolean requiereAval
) implements EventoDominio {}
