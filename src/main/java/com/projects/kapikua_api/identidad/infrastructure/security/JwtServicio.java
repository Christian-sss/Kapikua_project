package com.projects.kapikua_api.identidad.infrastructure.security;

import com.projects.kapikua_api.identidad.application.port.out.EmisorToken;
import com.projects.kapikua_api.identidad.domain.model.Rol;
import com.projects.kapikua_api.shared.UsuarioId;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtServicio implements EmisorToken {
    private static final Logger log = LoggerFactory.getLogger(JwtServicio.class);
    private static final String CLAIM_ROL = "rol";

    private final SecretKey clave;
    private final Duration duracion;
    private final Clock clock;

    public JwtServicio(@Value("${kapikua.jwt.secreto:}") String secreto,
                       @Value("${kapikua.sesion.minutos:15}") long minutos,
                       Clock clock) {
        this.clave = Keys.hmacShaKeyFor(bytesDelSecreto(secreto));
        this.duracion = Duration.ofMinutes(minutos);
        this.clock = clock;
    }

    public Duration duracion() {
        return duracion;
    }

    @Override
    public String emitir(UsuarioId usuarioId, Rol rol) {
        var ahora = clock.instant();
        return Jwts.builder()
                .subject(usuarioId.valor().toString())
                .claim(CLAIM_ROL, rol.name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(duracion)))
                .signWith(clave)
                .compact();
    }

    public Optional<UsuarioId> leer(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(clave)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new UsuarioId(Long.valueOf(claims.getSubject())));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static byte[] bytesDelSecreto(String secreto) {
        if (secreto == null || secreto.isBlank()) {
            log.warn("KAPIKUA_JWT_SECRETO no está definido: se usa un secreto aleatorio (solo para desarrollo)");
            var aleatorio = new byte[32];
            new SecureRandom().nextBytes(aleatorio);
            return aleatorio;
        }
        var bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("KAPIKUA_JWT_SECRETO debe tener al menos 32 caracteres");
        }
        return bytes;
    }
}
