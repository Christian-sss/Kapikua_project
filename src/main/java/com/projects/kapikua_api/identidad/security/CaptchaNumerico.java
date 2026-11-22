package com.projects.kapikua_api.identidad.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Captcha numérico del inicio de sesión: frena los intentos automatizados de adivinar contraseñas.
 * El número se entrega solo como imagen y cada reto sirve una sola vez, acierte o no el usuario.
 * Los retos viven en memoria, igual que los códigos de un solo uso.
 */
@Component
public class CaptchaNumerico {
    public record Reto(String id, String imagenBase64) {
    }

    private record Pendiente(String numero, Instant vence) {
    }

    static final int DIGITOS = 5;
    private static final int MAXIMO_PENDIENTES = 5_000;

    private static final int ANCHO = 190, ALTO = 64;
    private static final int ANCHO_DIGITO = 22, ALTO_DIGITO = 38, GROSOR = 5, PASO = 34;
    private static final int FONDO = 0xF3F5F9;
    private static final int[] TINTAS = {0x1D2433, 0x1F3A8A, 0x5B2A86, 0x14532D};
    private static final int[] RUIDO = {0x9AA7BD, 0xB7C0D1, 0x8FA3C7};

    /**
     * Segmentos encendidos de cada dígito: a (arriba), b, c (derecha), d (abajo), e, f (izquierda) y g (centro).
     */
    private static final String[] SEGMENTOS = {
            "abcdef", "bc", "abdeg", "abcdg", "bcfg", "acdfg", "acdefg", "abc", "abcdefg", "abcdfg"};

    private final Map<String, Pendiente> pendientes = new ConcurrentHashMap<>();
    private final SecureRandom aleatorio = new SecureRandom();
    private final Duration vigencia;
    private final Clock clock;

    public CaptchaNumerico(@Value("${kapikua.captcha.minutos:5}") long minutos, Clock clock) {
        this.vigencia = Duration.ofMinutes(minutos);
        this.clock = clock;
    }

    public Reto nuevo() {
        limpiar();
        var numero = String.format("%0" + DIGITOS + "d", aleatorio.nextInt((int) Math.pow(10, DIGITOS)));
        var id = UUID.randomUUID().toString();
        pendientes.put(id, new Pendiente(numero, clock.instant().plus(vigencia)));
        return new Reto(id, Base64.getEncoder().encodeToString(png(numero)));
    }

    /**
     * Consume el reto: una respuesta equivocada también lo anula y obliga a pedir otro.
     */
    public boolean validar(String id, String respuesta) {
        if (id == null || respuesta == null) return false;
        var pendiente = pendientes.remove(id);
        if (pendiente == null || !clock.instant().isBefore(pendiente.vence())) return false;
        return MessageDigest.isEqual(pendiente.numero().getBytes(StandardCharsets.UTF_8),
                respuesta.trim().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Solo para las pruebas: el número que espera un reto todavía vigente.
     */
    Optional<String> numeroEsperado(String id) {
        return Optional.ofNullable(pendientes.get(id)).map(Pendiente::numero);
    }

    private void limpiar() {
        var ahora = clock.instant();
        pendientes.values().removeIf(p -> !ahora.isBefore(p.vence()));
        if (pendientes.size() >= MAXIMO_PENDIENTES) pendientes.clear();
    }

    /**
     * Dibuja los dígitos como segmentos, píxel a píxel y sin fuentes del sistema, para que funcione
     * en un contenedor sin entorno gráfico. Cada dígito se inclina y se desplaza, y se agregan líneas
     * y puntos de ruido.
     */
    private byte[] png(String numero) {
        var imagen = new BufferedImage(ANCHO, ALTO, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < ALTO; y++) for (int x = 0; x < ANCHO; x++) imagen.setRGB(x, y, FONDO);

        for (int i = 0; i < 6; i++) {
            linea(imagen, aleatorio.nextInt(ANCHO), aleatorio.nextInt(ALTO), aleatorio.nextInt(ANCHO),
                    aleatorio.nextInt(ALTO), RUIDO[aleatorio.nextInt(RUIDO.length)]);
        }
        for (int i = 0; i < numero.length(); i++) {
            int x = 12 + i * PASO + aleatorio.nextInt(7) - 3;
            int y = (ALTO - ALTO_DIGITO) / 2 + aleatorio.nextInt(13) - 6;
            double inclinacion = (aleatorio.nextDouble() - 0.5) * 0.5;
            digito(imagen, numero.charAt(i) - '0', x, y, inclinacion, TINTAS[aleatorio.nextInt(TINTAS.length)]);
        }
        for (int i = 0; i < 2; i++) {
            linea(imagen, 0, aleatorio.nextInt(ALTO), ANCHO - 1, aleatorio.nextInt(ALTO),
                    TINTAS[aleatorio.nextInt(TINTAS.length)]);
        }
        for (int i = 0; i < 220; i++) {
            punto(imagen, aleatorio.nextInt(ANCHO), aleatorio.nextInt(ALTO), RUIDO[aleatorio.nextInt(RUIDO.length)]);
        }

        try (var salida = new ByteArrayOutputStream()) {
            ImageIO.write(imagen, "png", salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar la imagen del captcha", e);
        }
    }

    private void digito(BufferedImage imagen, int digito, int x, int y, double inclinacion, int color) {
        int mitad = (ALTO_DIGITO - GROSOR) / 2;
        for (char segmento : SEGMENTOS[digito].toCharArray()) {
            switch (segmento) {
                case 'a' -> rectangulo(imagen, x, y, ANCHO_DIGITO, GROSOR, y, inclinacion, color);
                case 'g' -> rectangulo(imagen, x, y + mitad, ANCHO_DIGITO, GROSOR, y, inclinacion, color);
                case 'd' ->
                        rectangulo(imagen, x, y + ALTO_DIGITO - GROSOR, ANCHO_DIGITO, GROSOR, y, inclinacion, color);
                case 'f' -> rectangulo(imagen, x, y, GROSOR, mitad + GROSOR, y, inclinacion, color);
                case 'b' ->
                        rectangulo(imagen, x + ANCHO_DIGITO - GROSOR, y, GROSOR, mitad + GROSOR, y, inclinacion, color);
                case 'e' -> rectangulo(imagen, x, y + mitad, GROSOR, mitad + GROSOR, y, inclinacion, color);
                case 'c' ->
                        rectangulo(imagen, x + ANCHO_DIGITO - GROSOR, y + mitad, GROSOR, mitad + GROSOR, y, inclinacion, color);
                default -> throw new IllegalStateException("Segmento desconocido: " + segmento);
            }
        }
    }

    private void rectangulo(BufferedImage imagen, int x, int y, int ancho, int alto, int base, double inclinacion, int color) {
        for (int fila = y; fila < y + alto; fila++) {
            int corrimiento = (int) Math.round((fila - base - ALTO_DIGITO / 2.0) * inclinacion);
            for (int columna = x; columna < x + ancho; columna++) punto(imagen, columna + corrimiento, fila, color);
        }
    }

    private void linea(BufferedImage imagen, int x0, int y0, int x1, int y1, int color) {
        int pasos = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= pasos; i++) {
            double t = pasos == 0 ? 0 : (double) i / pasos;
            punto(imagen, (int) Math.round(x0 + (x1 - x0) * t), (int) Math.round(y0 + (y1 - y0) * t), color);
        }
    }

    private void punto(BufferedImage imagen, int x, int y, int color) {
        if (x >= 0 && x < ANCHO && y >= 0 && y < ALTO) imagen.setRGB(x, y, color);
    }
}
