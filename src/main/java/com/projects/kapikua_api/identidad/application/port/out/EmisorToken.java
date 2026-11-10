package com.projects.kapikua_api.identidad.application.port.out;

import com.projects.kapikua_api.identidad.domain.model.Rol;
import com.projects.kapikua_api.shared.UsuarioId;

/** Emite el token firmado de la sesión (RNF-03). */
public interface EmisorToken {

    String emitir(UsuarioId usuarioId, Rol rol);
}
