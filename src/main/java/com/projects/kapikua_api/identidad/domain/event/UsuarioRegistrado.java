package com.projects.kapikua_api.identidad.domain.event;

import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.domain.EventoDominio;

public record UsuarioRegistrado(UsuarioId usuarioId, String celular, String nombreNegocio) implements EventoDominio {}
