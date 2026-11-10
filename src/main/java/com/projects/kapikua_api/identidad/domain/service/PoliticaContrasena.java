package com.projects.kapikua_api.identidad.domain.service;

import com.projects.kapikua_api.identidad.domain.model.*;

import com.projects.kapikua_api.identidad.domain.error.RegistroError;
import com.projects.kapikua_api.shared.Result;

public final class PoliticaContrasena {
    private static final int LONGITUD_MINIMA = 8;

    private PoliticaContrasena() {}

    public static Result<Void, RegistroError> validar(String clave) {
        if (clave == null || clave.length() < LONGITUD_MINIMA
                || clave.chars().noneMatch(Character::isLetter)
                || clave.chars().noneMatch(Character::isDigit)) {
            return Result.err(new RegistroError.ContrasenaDebil());
        }
        return Result.ok();
    }
}
