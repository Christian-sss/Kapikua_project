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
 * Cookie que lleva el JWT: HttpOnly (el JavaScript no la lee), Secure y SameSite=Strict (SAD 1.3).
 * Los navegadores aceptan cookies Secure en http://localhost, así que no hace falta desactivarlo en desarrollo.
 */
@Component
public class CookieSesion {

    public static final String NOMBRE = "KAPIKUA_SESION";

    private final boolean segura;

    public CookieSesion(@Value("${kapikua.sesion.cookie-segura:true}") boolean segura) {
        this.segura = segura;
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

    public void escribir(HttpServletResponse response, String token, Duration duracion) {
        response.addHeader(HttpHeaders.SET_COOKIE, construir(token, duracion).toString());
    }

    public void borrar(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, construir("", Duration.ZERO).toString());
    }

    private ResponseCookie construir(String valor, Duration duracion) {
        return ResponseCookie.from(NOMBRE, valor)
                .httpOnly(true)
                .secure(segura)
                .sameSite("Strict")
                .path("/")
                .maxAge(duracion)
                .build();
    }
}
