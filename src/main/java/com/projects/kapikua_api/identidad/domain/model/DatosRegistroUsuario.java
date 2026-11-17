package com.projects.kapikua_api.identidad.domain.model;

import com.projects.kapikua_api.identidad.domain.error.RegistroError;
import com.projects.kapikua_api.shared.Result;

import java.time.LocalDate;
import java.util.regex.Pattern;

public record DatosRegistroUsuario(String dni, String correo, LocalDate fechaNacimiento) {
    private static final Pattern DNI = Pattern.compile("^[0-9]{8}$");
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public DatosRegistroUsuario {
        correo = correo == null ? "" : correo.trim().toLowerCase();
    }

    Result<Void, RegistroError> validar(LocalDate hoy) {
        if (dni == null || !DNI.matcher(dni).matches()) {
            return Result.err(new RegistroError.DatoInvalido("DNI"));
        }
        if (!CORREO.matcher(correo).matches()) {
            return Result.err(new RegistroError.DatoInvalido("correo"));
        }
        if (fechaNacimiento == null || fechaNacimiento.isAfter(hoy)) {
            return Result.err(new RegistroError.DatoInvalido("fecha de nacimiento"));
        }
        return Result.ok();
    }

    @Override
    public String toString() {
        return "DatosRegistroUsuario[dni=****]";
    }
}
