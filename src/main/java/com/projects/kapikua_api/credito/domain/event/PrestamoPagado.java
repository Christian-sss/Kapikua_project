package com.projects.kapikua_api.credito.domain.event;

import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.domain.EventoDominio;

public record PrestamoPagado(UsuarioId usuarioId, Long prestamoId, Long ofertaId, boolean pagadoPorAval)
        implements EventoDominio {}
