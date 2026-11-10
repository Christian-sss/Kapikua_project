package com.projects.kapikua_api.shared.puerto;

import com.projects.kapikua_api.shared.UsuarioId;

/**
 * Avisos a la pantalla del usuario (RNF-17). En la v1 no hay correo, SMS ni push: los avisos solo llegan
 * a las pantallas abiertas. Se llama DESPUÉS de confirmar la transacción, para no avisar de un cambio revertido.
 * El aviso no lleva datos: la pantalla vuelve a pedir lo que necesita, ya con los permisos del usuario.
 */
public interface Notificador {

    enum Aviso {
        /** Cambió el saldo o los movimientos de la billetera. */
        BILLETERA
    }

    void notificar(UsuarioId usuarioId, Aviso aviso);
}
