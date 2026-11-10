package com.projects.kapikua_api.identidad.application.port.out;

import com.projects.kapikua_api.identidad.domain.model.IdentidadVerificada;

import java.time.LocalDate;
import java.util.Optional;

/** Puerto hacia RENIEC y las listas restrictivas (simulados en la v1). */
public interface ValidadorIdentidad {

    /** Vacío si RENIEC no confirma el DNI con esa fecha de nacimiento. */
    Optional<IdentidadVerificada> validar(String dni, LocalDate fechaNacimiento);

    boolean figuraEnListas(String dni);
}
