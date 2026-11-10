package com.projects.kapikua_api.identidad.infrastructure.security;

import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.puerto.CodigoUnSoloUso.CodigoError;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static com.projects.kapikua_api.shared.puerto.CodigoUnSoloUso.Proposito.ACEPTAR_OFERTA;
import static com.projects.kapikua_api.shared.puerto.CodigoUnSoloUso.Proposito.TRANSFERENCIA;
import static org.assertj.core.api.Assertions.assertThat;

class CodigoUnSoloUsoEnMemoriaTest {
    private static final UsuarioId ANA = new UsuarioId(1L);
    private static final String DETALLE = "987654321|50.00|ref-1";

    private static final class Reloj extends Clock {
        Instant ahora = Instant.parse("2026-11-09T15:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return ahora; }
    }

    private final Reloj reloj = new Reloj();
    private final CodigoUnSoloUsoEnMemoria codigos = new CodigoUnSoloUsoEnMemoria(5, reloj);

    @Test
    void elCodigoCorrectoSirveUnaSolaVez() {
        var codigo = codigos.generar(ANA, TRANSFERENCIA, DETALLE);
        assertThat(codigo).matches("\\d{6}");
        assertThat(codigos.validar(ANA, TRANSFERENCIA, DETALLE, codigo)).isInstanceOf(Result.Ok.class);
        assertThat(error(codigos.validar(ANA, TRANSFERENCIA, DETALLE, codigo))).isInstanceOf(CodigoError.Vencido.class);
    }

    @Test
    void venceALosCincoMinutos() {
        var codigo = codigos.generar(ANA, TRANSFERENCIA, DETALLE);
        reloj.ahora = reloj.ahora.plus(Duration.ofMinutes(5));
        assertThat(error(codigos.validar(ANA, TRANSFERENCIA, DETALLE, codigo))).isInstanceOf(CodigoError.Vencido.class);
    }

    @Test
    void noSirveParaOtraOperacionNiOtroProposito() {
        var codigo = codigos.generar(ANA, TRANSFERENCIA, DETALLE);
        assertThat(error(codigos.validar(ANA, TRANSFERENCIA, "987654321|500.00|ref-1", codigo)))
                .isInstanceOf(CodigoError.Invalido.class);
        assertThat(error(codigos.validar(ANA, ACEPTAR_OFERTA, DETALLE, codigo))).isInstanceOf(CodigoError.Vencido.class);
        assertThat(codigos.validar(ANA, TRANSFERENCIA, DETALLE, codigo)).isInstanceOf(Result.Ok.class);
    }

    @Test
    void seAnulaTrasTresIntentosFallidos() {
        var codigo = codigos.generar(ANA, TRANSFERENCIA, DETALLE);
        var malo = codigo.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 3; i++) {
            assertThat(error(codigos.validar(ANA, TRANSFERENCIA, DETALLE, malo))).isInstanceOf(CodigoError.Invalido.class);
        }
        assertThat(error(codigos.validar(ANA, TRANSFERENCIA, DETALLE, codigo))).isInstanceOf(CodigoError.Vencido.class);
    }

    private static CodigoError error(Result<Void, CodigoError> r) {
        assertThat(r).isInstanceOf(Result.Err.class);
        return ((Result.Err<Void, CodigoError>) r).error();
    }
}
