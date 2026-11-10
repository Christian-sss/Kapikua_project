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
