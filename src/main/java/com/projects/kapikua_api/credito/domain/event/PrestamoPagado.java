package com.projects.kapikua_api.credito.domain.event;

import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.domain.EventoDominio;

/** El préstamo quedó pagado. {@code pagadoPorAval}: el aval tuvo que pagar alguna cuota (RN-32). */
public record PrestamoPagado(UsuarioId usuarioId, Long prestamoId, Long ofertaId, boolean pagadoPorAval)
        implements EventoDominio {}
