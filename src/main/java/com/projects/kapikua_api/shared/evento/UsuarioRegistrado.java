package com.projects.kapikua_api.shared.evento;

import com.projects.kapikua_api.shared.UsuarioId;

public record UsuarioRegistrado(UsuarioId usuarioId, String celular, String nombreNegocio) {}
