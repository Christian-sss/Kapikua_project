package com.projects.kapikua_api.identidad.domain.service;

import com.projects.kapikua_api.identidad.domain.model.*;

import com.projects.kapikua_api.shared.Result;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PoliticaContrasenaTest {

    @ParameterizedTest
    @ValueSource(strings = {"abc12345", "Kapikua2026", "a1b2c3d4"})
    void aceptaOchoOMasConLetrasYNumeros(String clave) {
        assertThat(PoliticaContrasena.validar(clave)).isInstanceOf(Result.Ok.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc1234", "abcdefgh", "12345678", ""})
    void rechazaCortasOSinLetrasOSinNumeros(String clave) {
        assertThat(PoliticaContrasena.validar(clave)).isInstanceOf(Result.Err.class);
    }
}
