package com.projects.kapikua_api.identidad.application.port.out;

import com.projects.kapikua_api.identidad.domain.model.IdentidadVerificada;

import java.time.LocalDate;
import java.util.Optional;

public interface ValidadorIdentidad {
    Optional<IdentidadVerificada> validar(String dni, LocalDate fechaNacimiento);

    boolean figuraEnListas(String dni);
}
