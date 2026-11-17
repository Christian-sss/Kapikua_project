package com.projects.kapikua_api.identidad.infrastructure.security;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

class CaptchaNumericoTest {
    private static final class Reloj extends Clock {
        Instant ahora = Instant.parse("2026-11-17T15:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return ahora; }
    }

    private final Reloj reloj = new Reloj();
    private final CaptchaNumerico captcha = new CaptchaNumerico(5, reloj);

    private String numero(CaptchaNumerico.Reto reto) {
        return captcha.numeroEsperado(reto.id()).orElseThrow();
    }

    private static String otro(String numero) {
        return numero.equals("00000") ? "11111" : "00000";
    }

    @Test
    void elNumeroCorrectoSirveUnaSolaVez() {
        var reto = captcha.nuevo();
        var numero = numero(reto);
        assertThat(numero).matches("\\d{5}");
        assertThat(captcha.validar(reto.id(), " " + numero + " ")).isTrue();
        assertThat(captcha.validar(reto.id(), numero)).isFalse();
    }

    @Test
    void unaRespuestaEquivocadaAnulaElReto() {
        var reto = captcha.nuevo();
        var numero = numero(reto);
        assertThat(captcha.validar(reto.id(), otro(numero))).isFalse();
        assertThat(captcha.validar(reto.id(), numero)).isFalse();
    }

    @Test
    void venceALosCincoMinutos() {
        var reto = captcha.nuevo();
        var numero = numero(reto);
        reloj.ahora = reloj.ahora.plus(Duration.ofMinutes(5));
        assertThat(captcha.validar(reto.id(), numero)).isFalse();
    }

    @Test
    void sinRetoOSinRespuestaNoPasa() {
        var reto = captcha.nuevo();
        assertThat(captcha.validar(null, numero(reto))).isFalse();
        assertThat(captcha.validar("no-existe", "12345")).isFalse();
        assertThat(captcha.validar(reto.id(), null)).isFalse();
    }

    @Test
    void elNumeroDeUnRetoNoSirveParaOtro() {
        var uno = captcha.nuevo();
        var dos = captcha.nuevo();
        var numeroUno = numero(uno);
        assertThat(uno.id()).isNotEqualTo(dos.id());
        if (!numeroUno.equals(numero(dos))) assertThat(captcha.validar(dos.id(), numeroUno)).isFalse();
        assertThat(captcha.validar(uno.id(), numeroUno)).isTrue();
    }

    @Test
    void laImagenEsUnPngQueNoRevelaElNumeroComoTexto() throws Exception {
        var reto = captcha.nuevo();
        var bytes = Base64.getDecoder().decode(reto.imagenBase64());
        var imagen = ImageIO.read(new ByteArrayInputStream(bytes));
        assertThat(imagen.getWidth()).isEqualTo(190);
        assertThat(imagen.getHeight()).isEqualTo(64);

        var colores = new HashSet<Integer>();
        for (int y = 0; y < imagen.getHeight(); y++) {
            for (int x = 0; x < imagen.getWidth(); x++) colores.add(imagen.getRGB(x, y));
        }
        assertThat(colores).as("fondo, dígitos y ruido").hasSizeGreaterThan(2);
        assertThat(new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1)).doesNotContain(numero(reto));
        assertThat(reto.toString()).doesNotContain(numero(reto));
    }

    @Test
    void losRetosVencidosSeLimpianAlPedirOtro() {
        var viejo = captcha.nuevo();
        reloj.ahora = reloj.ahora.plus(Duration.ofMinutes(6));
        captcha.nuevo();
        assertThat(captcha.numeroEsperado(viejo.id())).isEmpty();
    }
}
