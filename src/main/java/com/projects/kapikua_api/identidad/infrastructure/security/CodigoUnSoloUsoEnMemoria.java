package com.projects.kapikua_api.identidad.infrastructure.security;

import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.puerto.CodigoUnSoloUso;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Código de un solo uso de 6 dígitos (RNF-04), en memoria como indica el script v6.
 * Hay un código vigente por usuario y propósito: pedir otro reemplaza al anterior.
 * Vence a los {@code kapikua.codigo.minutos} (5), se consume al validarse bien y se anula tras 3 intentos fallidos.
 * Con más de una instancia de la aplicación habría que llevarlo a un almacén compartido.
 */
@Component
class CodigoUnSoloUsoEnMemoria implements CodigoUnSoloUso {

    private static final int INTENTOS_MAXIMOS = 3;

    private record Clave(UsuarioId usuarioId, Proposito proposito) {}

    private static final class Entrada {
        final String codigo;
        final String detalle;
        final Instant vence;
        int intentosFallidos;

        Entrada(String codigo, String detalle, Instant vence) {
            this.codigo = codigo;
            this.detalle = detalle;
            this.vence = vence;
        }
    }

    private final Map<Clave, Entrada> vigentes = new ConcurrentHashMap<>();
    private final SecureRandom aleatorio = new SecureRandom();
    private final Duration vigencia;
    private final Clock clock;

    CodigoUnSoloUsoEnMemoria(@Value("${kapikua.codigo.minutos:5}") long minutos, Clock clock) {
        this.vigencia = Duration.ofMinutes(minutos);
        this.clock = clock;
    }

    @Override
    public String generar(UsuarioId usuarioId, Proposito proposito, String detalleOperacion) {
        var codigo = String.format("%06d", aleatorio.nextInt(1_000_000));
        vigentes.put(new Clave(usuarioId, proposito),
                new Entrada(codigo, detalleOperacion, clock.instant().plus(vigencia)));
        return codigo;
    }

    @Override
    public Result<Void, CodigoError> validar(UsuarioId usuarioId, Proposito proposito, String detalleOperacion,
                                             String codigo) {
        var clave = new Clave(usuarioId, proposito);
        // compute: la lectura, el conteo de intentos y el consumo son atómicos por usuario y propósito.
        var resultado = new AtomicReference<Result<Void, CodigoError>>();
        vigentes.compute(clave, (k, entrada) -> {
            if (entrada == null || !clock.instant().isBefore(entrada.vence)) {
                resultado.set(Result.err(new CodigoError.Vencido()));
                return null;
            }
            if (iguales(entrada.codigo, codigo) && entrada.detalle.equals(detalleOperacion)) {
                resultado.set(Result.ok());
                return null;
            }
            entrada.intentosFallidos++;
            resultado.set(Result.err(new CodigoError.Invalido()));
            return entrada.intentosFallidos >= INTENTOS_MAXIMOS ? null : entrada;
        });
        return resultado.get();
    }

    private static boolean iguales(String esperado, String recibido) {
        if (recibido == null) return false;
        return MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8),
                recibido.trim().getBytes(StandardCharsets.UTF_8));
    }
}
