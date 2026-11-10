package com.projects.kapikua_api.billetera.application.port.out;

import com.projects.kapikua_api.shared.UsuarioId;

public interface Titulares {
    String nombreEnmascarado(UsuarioId usuarioId);

    String correo(UsuarioId usuarioId);
}
