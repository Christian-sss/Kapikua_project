package com.projects.kapikua_api.identidad.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** La cookie de sesión es Secure por HTTPS y no por HTTP local, donde Safari la descartaría. */
class CookieSesionTest {

    static String cookieEscrita(boolean siempreSegura, boolean https) {
        var request = new MockHttpServletRequest();
        request.setSecure(https);
        var response = new MockHttpServletResponse();
        new CookieSesion(siempreSegura).escribir(request, response, "jwt", Duration.ofMinutes(15));
        return response.getHeader(HttpHeaders.SET_COOKIE);
    }

    @Test
    void porHttpsLaCookieEsSecure() {
        assertThat(cookieEscrita(false, true)).contains("Secure").contains("HttpOnly").contains("SameSite=Strict");
    }

    @Test
    void porHttpLocalNoSeMarcaSecureParaQueSafariLaAcepte() {
        assertThat(cookieEscrita(false, false)).doesNotContain("Secure").contains("HttpOnly").contains("SameSite=Strict");
    }

    @Test
    void conLaPropiedadActivaEsSecureSiempre() {
        assertThat(cookieEscrita(true, false)).contains("Secure");
    }

    @Test
    void alBorrarlaSeConservanLosMismosAtributos() {
        var request = new MockHttpServletRequest();
        request.setSecure(true);
        var response = new MockHttpServletResponse();
        new CookieSesion(false).borrar(request, response);
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0").contains("Secure");
    }
}
