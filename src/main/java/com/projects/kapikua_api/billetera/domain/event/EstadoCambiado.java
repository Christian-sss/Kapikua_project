package com.projects.kapikua_api.billetera.domain.event;

import com.projects.kapikua_api.shared.domain.EventoDominio;

import java.time.OffsetDateTime;

public record EstadoCambiado(
        Long billeteraId,
        String estadoAnterior,
        String estadoNuevo,
        String motivo,
        OffsetDateTime fecha
) implements EventoDominio {}





