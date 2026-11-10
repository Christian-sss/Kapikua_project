package com.projects.kapikua_api.billetera.application.port.out;

import com.projects.kapikua_api.shared.UsuarioId;

/** Datos del titular que viven en Identidad. */
public interface Titulares {

    /** "Ana Q.": nombre parcialmente oculto para confirmar una transferencia (CU-05, paso 3). */
    String nombreEnmascarado(UsuarioId usuarioId);

    /** Para el recibo de la pasarela de pagos. */
    String correo(UsuarioId usuarioId);
}
