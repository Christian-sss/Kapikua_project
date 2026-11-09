package com.projects.kapikua_api.identidad.application.command;

import java.time.LocalDate;

/**
 * Datos del formulario de registro (CU-01, paso 2). Los nombres no se piden:
 * los devuelve RENIEC. El celular y el nombre del negocio son para la billetera.
 */
public record RegistrarUsuarioCommand(
        String dni,
        LocalDate fechaNacimiento,
        String correo,
        String celular,
        String nombreNegocio,
        String contrasena,
        boolean aceptaTerminos
) {
    @Override
    public String toString() {
        return "RegistrarUsuarioCommand[dni=****, correo=" + correo + ", contrasena=****]";
    }
}
