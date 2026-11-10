package com.projects.kapikua_api.identidad.domain.error;

import com.projects.kapikua_api.shared.ErrorDominio;

/** Errores del inicio de sesión (CU-02). */
public sealed interface AutenticacionError extends ErrorDominio {

    /** Mensaje general a propósito: no indica qué dato falló (CU-02, 2a). */
    record CredencialesInvalidas() implements AutenticacionError {
        public String codigo()  { return "ID-10"; }
        public String mensaje() { return "Correo, DNI o contraseña incorrectos."; }
    }

    /** CU-02, 2b. Solo se informa cuando la contraseña es correcta. */
    record UsuarioInactivo() implements AutenticacionError {
        public String codigo()  { return "ID-11"; }
        public String mensaje() { return "Tu cuenta está inactiva. Comunícate con el Administrador."; }
    }
}
