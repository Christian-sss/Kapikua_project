package com.projects.kapikua_api.identidad.controller;

import com.projects.kapikua_api.identidad.dto.SesionEmitida;
import com.projects.kapikua_api.identidad.service.AutenticacionService;
import com.projects.kapikua_api.identidad.error.AutenticacionError;
import com.projects.kapikua_api.identidad.security.CaptchaNumerico;
import com.projects.kapikua_api.shared.Result;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.ExtendedModelMap;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class LoginControllerTest {
    /** Captcha de prueba: acepta solo la respuesta "ok" y cuenta los retos que entrega. */
    private static final class CaptchaDePrueba extends CaptchaNumerico {
        int entregados;

        CaptchaDePrueba() {
            super(5, Clock.systemUTC());
        }

        @Override
        public Reto nuevo() {
            entregados++;
            return new Reto("reto-" + entregados, "imagen");
        }

        @Override
        public boolean validar(String id, String respuesta) {
            return "ok".equals(respuesta);
        }
    }

    private final CaptchaDePrueba captcha = new CaptchaDePrueba();
    private final AtomicInteger intentos = new AtomicInteger();
    private final AutenticacionService credencialesMalas = new AutenticacionService(null, new ClaveSinCifrar(), null) {
        @Override
        public Result<SesionEmitida, AutenticacionError> autenticar(String usuario, String contrasena) {
            intentos.incrementAndGet();
            return Result.err(new AutenticacionError.CredencialesInvalidas());
        }
    };

    private static final class ClaveSinCifrar implements PasswordEncoder {
        public String encode(CharSequence clave) { return clave.toString(); }
        public boolean matches(CharSequence clave, String hash) { return hash.contentEquals(clave); }
    }
    private final LoginController controlador = new LoginController(credencialesMalas, null, null, captcha);

    @Test
    void elFormularioSiempreLlevaUnCaptchaNuevo() {
        var modelo = new ExtendedModelMap();
        assertThat(controlador.formulario(null, null, null, modelo)).isEqualTo("identidad/login");
        assertThat(modelo.get("captcha")).isEqualTo(new CaptchaNumerico.Reto("reto-1", "imagen"));
    }

    @Test
    void conElCaptchaEquivocadoNoSePruebanLasCredenciales() {
        var modelo = new ExtendedModelMap();
        var vista = controlador.ingresar("ana@kapikua.pe", "clave", "reto-0", "12345", modelo, null, null);

        assertThat(vista).isEqualTo("identidad/login");
        assertThat(intentos).hasValue(0);
        assertThat(modelo.get("error")).isEqualTo(LoginController.CAPTCHA_INCORRECTO);
        assertThat(modelo.get("usuario")).isEqualTo("ana@kapikua.pe");
        assertThat(modelo.get("captcha")).isEqualTo(new CaptchaNumerico.Reto("reto-1", "imagen"));
    }

    @Test
    void sinCaptchaTampocoSePruebanLasCredenciales() {
        var modelo = new ExtendedModelMap();
        controlador.ingresar("ana@kapikua.pe", "clave", null, null, modelo, null, null);
        assertThat(intentos).hasValue(0);
        assertThat(modelo.get("error")).isEqualTo(LoginController.CAPTCHA_INCORRECTO);
    }

    @Test
    void conElCaptchaCorrectoYCredencialesMalasSePideOtroCaptcha() {
        var modelo = new ExtendedModelMap();
        var vista = controlador.ingresar("ana@kapikua.pe", "mala", "reto-0", "ok", modelo, null, null);

        assertThat(vista).isEqualTo("identidad/login");
        assertThat(intentos).hasValue(1);
        assertThat(modelo.get("error")).isEqualTo(new AutenticacionError.CredencialesInvalidas().mensaje());
        assertThat(modelo.get("captcha")).isEqualTo(new CaptchaNumerico.Reto("reto-1", "imagen"));
    }
}
