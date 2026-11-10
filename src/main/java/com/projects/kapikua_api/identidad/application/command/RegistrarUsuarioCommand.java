package com.projects.kapikua_api.identidad.application.command;

import java.time.LocalDate;

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
