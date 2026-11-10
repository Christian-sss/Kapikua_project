package com.projects.kapikua_api.identidad.infrastructure.security;

import com.projects.kapikua_api.identidad.domain.model.Rol;
import com.projects.kapikua_api.shared.UsuarioId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServicioTest {

    private static final String SECRETO = "secreto-de-prueba-de-32-caracteres!!";
    private static final Instant T0 = Instant.parse("2026-11-09T15:00:00Z");
    private static final UsuarioId ANA = new UsuarioId(7L);

    private static JwtServicio en(Instant instante) {
        return new JwtServicio(SECRETO, 15, Clock.fixed(instante, ZoneId.of("America/Lima")));
    }

    @Test
    void leeElUsuarioDeUnTokenVigente() {
        var token = en(T0).emitir(ANA, Rol.CLIENTE);
        assertThat(en(T0.plus(Duration.ofMinutes(14))).leer(token)).contains(ANA);
    }

    @Test
    void elTokenVenceALos15MinutosSinRenovar() {
        var token = en(T0).emitir(ANA, Rol.CLIENTE);
        assertThat(en(T0.plus(Duration.ofMinutes(16))).leer(token)).isEmpty();
    }

    @Test
    void rechazaUnTokenAlteradoOFirmadoConOtroSecreto() {
        var token = en(T0).emitir(ANA, Rol.CLIENTE);
        assertThat(en(T0).leer(token.substring(0, token.length() - 2) + "xx")).isEmpty();

        var otro = new JwtServicio("otro-secreto-distinto-de-32-caracteres", 15, Clock.fixed(T0, ZoneId.of("UTC")));
        assertThat(otro.leer(token)).isEmpty();
        assertThat(en(T0).leer("basura")).isEmpty();
    }

    @Test
    void exigeUnSecretoDeAlMenos32Caracteres() {
        assertThatThrownBy(() -> new JwtServicio("corto", 15, Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class);
    }
}
