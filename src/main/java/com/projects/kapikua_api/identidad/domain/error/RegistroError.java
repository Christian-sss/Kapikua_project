package com.projects.kapikua_api.identidad.domain.error;

public sealed interface RegistroError {

    record MenorDeEdad() implements RegistroError {
    }

    record DniDuplicado() implements RegistroError {
    }

    record CelularDuplicado() implements RegistroError {
    }

}