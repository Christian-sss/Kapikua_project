package com.projects.kapikua_api.shared.puerto;

import com.projects.kapikua_api.shared.UsuarioId;

public interface Notificador {
    enum Aviso {
        BILLETERA
    }

    void notificar(UsuarioId usuarioId, Aviso aviso);
}
