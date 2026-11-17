package com.projects.kapikua_api.shared.puerto;

import com.projects.kapikua_api.shared.UsuarioId;

public interface Notificador {
    /** BILLETERA: saldo y movimientos. CREDITO: cuotas, préstamo y ofertas (RNF-17). */
    enum Aviso {
        BILLETERA,
        CREDITO
    }

    void notificar(UsuarioId usuarioId, Aviso aviso);
}
