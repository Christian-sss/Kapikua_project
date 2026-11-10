package com.projects.kapikua_api.identidad.infrastructure.adapter.in.web;

import com.projects.kapikua_api.identidad.application.port.in.AutenticarUseCase;
import com.projects.kapikua_api.identidad.application.port.in.AutenticarUseCase.SesionEmitida;
import com.projects.kapikua_api.identidad.domain.error.AutenticacionError;
import com.projects.kapikua_api.identidad.domain.model.Rol;
import com.projects.kapikua_api.identidad.infrastructure.security.CookieSesion;
import com.projects.kapikua_api.identidad.infrastructure.security.JwtServicio;
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

    private final AutenticarUseCase autenticar;
    private final CookieSesion cookie;
    private final JwtServicio jwt;

    LoginController(AutenticarUseCase autenticar, CookieSesion cookie, JwtServicio jwt) {
        this.autenticar = autenticar;
        this.cookie = cookie;
        this.jwt = jwt;
    }

    @GetMapping("/login")
    String formulario(@RequestParam(required = false) String vencida,
                      @RequestParam(required = false) String salir,
                      @RequestParam(required = false) String registrado,
                      Model model) {
        if (vencida != null) model.addAttribute("aviso", "Tu sesión venció por inactividad. Ingresa de nuevo.");
        if (salir != null) model.addAttribute("aviso", "Cerraste tu sesión.");
        if (registrado != null) model.addAttribute("aviso", "Tu cuenta fue creada. Ya puedes iniciar sesión.");
        return VISTA;
    }

    @PostMapping("/login")
    String ingresar(@RequestParam String usuario,
                    @RequestParam String contrasena,
                    Model model,
                    HttpServletRequest request,
                    HttpServletResponse response) {
        return switch (autenticar.autenticar(usuario, contrasena)) {
            case Result.Ok<SesionEmitida, AutenticacionError> ok -> {
                cookie.escribir(request, response, ok.valor().token(), jwt.duracion());
                yield "redirect:" + inicioDe(ok.valor().rol());
            }
            case Result.Err<SesionEmitida, AutenticacionError> err -> {
                model.addAttribute("error", err.error().mensaje());
                model.addAttribute("usuario", usuario);
                yield VISTA;
            }
        };
    }

    static String inicioDe(Rol rol) {
        return rol == Rol.ADMINISTRADOR ? "/admin" : "/";
    }
}
