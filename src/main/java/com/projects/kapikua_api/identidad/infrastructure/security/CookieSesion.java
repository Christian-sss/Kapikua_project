package com.projects.kapikua_api.identidad.infrastructure.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Cookie que lleva el JWT: HttpOnly (el JavaScript no la lee), SameSite=Strict y Secure (SAD 1.3).
 * Secure se decide por la conexión: va siempre que la petición llegó por HTTPS (Railway termina el TLS en su
 * proxy y avisa con X-Forwarded-Proto: ver server.forward-headers-strategy). En http://localhost no se marca,
 * porque Safari descarta las cookies Secure que llegan por HTTP y el inicio de sesión fallaría sin aviso.
 * {@code kapikua.sesion.cookie-segura=true} la fuerza siempre, por ejemplo en producción.
 */
@Component
public class CookieSesion {

    public static final String NOMBRE = "KAPIKUA_SESION";

    private final boolean siempreSegura;

    public CookieSesion(@Value("${kapikua.sesion.cookie-segura:false}") boolean siempreSegura) {
        this.siempreSegura = siempreSegura;
    }

    public Optional<String> leer(HttpServletRequest request) {
        var cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
                .filter(c -> NOMBRE.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> !v.isBlank())
                .findFirst();
    }

    public void escribir(HttpServletRequest request, HttpServletResponse response, String token, Duration duracion) {
        response.addHeader(HttpHeaders.SET_COOKIE, construir(token, duracion, segura(request)).toString());
    }

    public void borrar(HttpServletRequest request, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, construir("", Duration.ZERO, segura(request)).toString());
    }

    boolean segura(HttpServletRequest request) {
        return siempreSegura || request.isSecure();
    }

    private static ResponseCookie construir(String valor, Duration duracion, boolean segura) {
        return ResponseCookie.from(NOMBRE, valor)
                .httpOnly(true)
                .secure(segura)
                .sameSite("Strict")
                .path("/")
                .maxAge(duracion)
                .build();
    }
}
