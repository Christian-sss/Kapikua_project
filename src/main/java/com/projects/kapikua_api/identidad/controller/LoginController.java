package com.projects.kapikua_api.identidad.controller;

import com.projects.kapikua_api.identidad.service.AutenticacionService;
import com.projects.kapikua_api.identidad.dto.SesionEmitida;
import com.projects.kapikua_api.identidad.error.AutenticacionError;
import com.projects.kapikua_api.identidad.entity.Rol;
import com.projects.kapikua_api.identidad.security.CaptchaNumerico;
import com.projects.kapikua_api.identidad.security.CookieSesion;
import com.projects.kapikua_api.identidad.security.JwtServicio;
import com.projects.kapikua_api.shared.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
class LoginController {

    private static final String VISTA = "identidad/login";
    static final String CAPTCHA_INCORRECTO = "El número de verificación no es correcto o venció. Inténtalo de nuevo.";

    private final AutenticacionService autenticacion;
    private final CookieSesion cookie;
    private final JwtServicio jwt;
    private final CaptchaNumerico captcha;

    LoginController(AutenticacionService autenticacion, CookieSesion cookie, JwtServicio jwt, CaptchaNumerico captcha) {
        this.autenticacion = autenticacion;
        this.cookie = cookie;
        this.jwt = jwt;
        this.captcha = captcha;
    }

    @GetMapping("/login")
    String formulario(@RequestParam(required = false) String vencida,
                      @RequestParam(required = false) String salir,
                      @RequestParam(required = false) String registrado,
                      Model model) {
        if (vencida != null) model.addAttribute("aviso", "Tu sesión venció por inactividad. Ingresa de nuevo.");
        if (salir != null) model.addAttribute("aviso", "Cerraste tu sesión.");
        if (registrado != null) model.addAttribute("aviso", "Tu cuenta fue creada. Ya puedes iniciar sesión.");
        return conCaptchaNuevo(model);
    }

    @PostMapping("/login")
    String ingresar(@RequestParam String usuario,
                    @RequestParam String contrasena,
                    @RequestParam(required = false) String captchaId,
                    @RequestParam(name = "captcha", required = false) String respuestaCaptcha,
                    Model model,
                    HttpServletRequest request,
                    HttpServletResponse response) {
        // El captcha se valida antes que las credenciales: un robot no llega a probar contraseñas.
        if (!captcha.validar(captchaId, respuestaCaptcha)) {
            model.addAttribute("error", CAPTCHA_INCORRECTO);
            model.addAttribute("usuario", usuario);
            return conCaptchaNuevo(model);
        }
        return switch (autenticacion.autenticar(usuario, contrasena)) {
            case Result.Ok<SesionEmitida, AutenticacionError> ok -> {
                cookie.escribir(request, response, ok.valor().token(), jwt.duracion());
                yield "redirect:" + inicioDe(ok.valor().rol());
            }
            case Result.Err<SesionEmitida, AutenticacionError> err -> {
                model.addAttribute("error", err.error().mensaje());
                model.addAttribute("usuario", usuario);
                yield conCaptchaNuevo(model);
            }
        };
    }

    private String conCaptchaNuevo(Model model) {
        model.addAttribute("captcha", captcha.nuevo());
        return VISTA;
    }

    static String inicioDe(Rol rol) {
        return rol == Rol.ADMINISTRADOR ? "/admin" : "/";
    }
}
