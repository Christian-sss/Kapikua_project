package com.projects.kapikua_api.credito.domain.event;

import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.domain.EventoDominio;

public record PrestamoDesembolsado(UsuarioId usuarioId, Long ofertaId, String codigoCredito, Dinero monto)
        implements EventoDominio {}
