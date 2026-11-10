package com.projects.kapikua_api.identidad.domain.error;

import com.projects.kapikua_api.shared.ErrorDominio;

public sealed interface RegistroError extends ErrorDominio {
    record DatoInvalido(String campo) implements RegistroError {
        public String codigo()  { return "ID-01"; }
        public String mensaje() { return "El dato ingresado no es válido: " + campo + "."; }
    }

    record ContrasenaDebil() implements RegistroError {
        public String codigo()  { return "ID-02"; }
        public String mensaje() { return "La contraseña debe tener al menos 8 caracteres e incluir letras y números."; }
    }

    record TerminosNoAceptados() implements RegistroError {
        public String codigo()  { return "ID-03"; }
        public String mensaje() { return "Debes aceptar los términos del servicio y la política de tratamiento de datos."; }
    }

    record DniDuplicado() implements RegistroError {
        public String codigo()  { return "ID-04"; }
        public String mensaje() { return "El DNI ya está registrado."; }
    }

    record CorreoDuplicado() implements RegistroError {
        public String codigo()  { return "ID-05"; }
        public String mensaje() { return "El correo ya está registrado."; }
    }

    record CelularDuplicado() implements RegistroError {
        public String codigo()  { return "ID-06"; }
        public String mensaje() { return "El número de celular ya está registrado."; }
    }

    record DniNoValidado() implements RegistroError {
        public String codigo()  { return "ID-07"; }
        public String mensaje() { return "No pudimos validar tu identidad con RENIEC. Revisa tu DNI y tu fecha de nacimiento."; }
    }

    record MenorDeEdad() implements RegistroError {
        public String codigo()  { return "ID-08"; }
        public String mensaje() { return "Debes ser mayor de 18 años para registrarte."; }
    }

    record EnListasRestrictivas() implements RegistroError {
        public String codigo()  { return "ID-09"; }
        public String mensaje() { return "No es posible completar el registro. Comunícate con atención al cliente."; }
    }
}
