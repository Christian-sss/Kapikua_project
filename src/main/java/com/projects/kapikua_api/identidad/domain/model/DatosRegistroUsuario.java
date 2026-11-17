package com.projects.kapikua_api.identidad.domain.model;

import com.projects.kapikua_api.identidad.domain.error.RegistroError;
import com.projects.kapikua_api.shared.Result;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

public record DatosRegistroUsuario(String dni, String correo, String celular, LocalDate fechaNacimiento) {
    public static final String FORMATO_CORREO =
            "^[A-Za-z0-9_%+-]+(?:\\.[A-Za-z0-9_%+-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+[A-Za-z]{2,}$";

    private static final Pattern DNI = Pattern.compile("^[0-9]{8}$");
    private static final Pattern CELULAR = Pattern.compile("^9[0-9]{8}$");
    private static final Pattern CORREO = Pattern.compile(FORMATO_CORREO);
    public static final List<String> DOMINIOS_CORREO_PERMITIDOS =
            List.of("gmail.com", "outlook.com", "kapikua.com", "upt.pe", "virtual.upt.pe");
    private static final String ASCENDENTE = "0123456789";
    private static final String DESCENDENTE = "9876543210";

    public DatosRegistroUsuario {
        correo = correo == null ? "" : correo.trim().toLowerCase();
        celular = celular == null ? "" : celular.replaceAll("\\s", "");
    }

    Result<Void, RegistroError> validar(LocalDate hoy) {
        if (dni == null || !DNI.matcher(dni).matches() || esNumeroFicticio(dni)) {
            return Result.err(new RegistroError.DatoInvalido("DNI"));
        }
        if (!CELULAR.matcher(celular).matches() || esNumeroFicticio(celular.substring(1))) {
            return Result.err(new RegistroError.DatoInvalido("celular"));
        }
        if (!CORREO.matcher(correo).matches()) {
            return Result.err(new RegistroError.DatoInvalido("correo"));
        }
        if (!DOMINIOS_CORREO_PERMITIDOS.contains(correo.substring(correo.indexOf('@') + 1))) {
            return Result.err(new RegistroError.DominioCorreoNoPermitido(DOMINIOS_CORREO_PERMITIDOS));
        }
        if (fechaNacimiento == null || fechaNacimiento.isAfter(hoy)) {
            return Result.err(new RegistroError.DatoInvalido("fecha de nacimiento"));
        }
        return Result.ok();
    }

    /** Todos los dígitos iguales (11111111) o seguidos (12345678, 87654321). */
    private static boolean esNumeroFicticio(String digitos) {
        return digitos.chars().distinct().count() == 1
                || ASCENDENTE.contains(digitos)
                || DESCENDENTE.contains(digitos);
    }

    @Override
    public String toString() {
        return "DatosRegistroUsuario[dni=****, celular=****]";
    }
}
