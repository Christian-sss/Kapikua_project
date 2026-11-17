package com.projects.kapikua_api.identidad.application.command;

import java.time.LocalDate;

/**
 * Datos del formulario de registro (CU-01). Los nombres y apellidos los escribe el cliente tal como figuran
 * en su DNI y se contrastan con RENIEC; si llegan vacíos se usan los que devuelva RENIEC.
 */
public record RegistrarUsuarioCommand(
        String dni,
        LocalDate fechaNacimiento,
        String nombres,
        String apellidos,
        String correo,
        String celular,
        String nombreNegocio,
        String contrasena,
        boolean aceptaTerminos
) {
    /** Registro sin nombres escritos: se toman los que devuelva RENIEC. */
    public RegistrarUsuarioCommand(String dni, LocalDate fechaNacimiento, String correo, String celular,
                                   String nombreNegocio, String contrasena, boolean aceptaTerminos) {
        this(dni, fechaNacimiento, null, null, correo, celular, nombreNegocio, contrasena, aceptaTerminos);
    }

    @Override
    public String toString() {
        return "RegistrarUsuarioCommand[dni=****, correo=" + correo + ", contrasena=****]";
    }
}
