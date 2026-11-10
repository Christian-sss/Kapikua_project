package com.projects.kapikua_api.identidad.domain.error;

import com.projects.kapikua_api.shared.ErrorDominio;

public sealed interface AutenticacionError extends ErrorDominio {
    record CredencialesInvalidas() implements AutenticacionError {
        public String codigo()  { return "ID-10"; }
        public String mensaje() { return "Correo, DNI o contraseña incorrectos."; }
    }

    record UsuarioInactivo() implements AutenticacionError {
        public String codigo()  { return "ID-11"; }
        public String mensaje() { return "Tu cuenta está inactiva. Comunícate con el Administrador."; }
    }
}
