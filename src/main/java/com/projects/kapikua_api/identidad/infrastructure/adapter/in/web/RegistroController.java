package com.projects.kapikua_api.identidad.infrastructure.adapter.in.web;

import com.projects.kapikua_api.identidad.application.port.in.RegistrarUsuarioUseCase;
import com.projects.kapikua_api.identidad.domain.error.RegistroError;
import com.projects.kapikua_api.identidad.domain.model.DatosRegistroUsuario;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/registro")
class RegistroController {
    private static final String VISTA = "identidad/registro";

    private final RegistrarUsuarioUseCase registrarUsuario;

    public RegistroController(RegistrarUsuarioUseCase registrarUsuario) {
        this.registrarUsuario = registrarUsuario;
    }

    @ModelAttribute("dominiosCorreo")
    List<String> dominiosCorreo() {
        return DatosRegistroUsuario.DOMINIOS_CORREO_PERMITIDOS;
    }

    @GetMapping
    String formulario(Model model) {
        model.addAttribute("form", new RegistroForm());
        return VISTA;
    }

    @PostMapping
    String registrar(@Valid @ModelAttribute("form") RegistroForm form,
                     BindingResult validacion,
                     Model model) {
        if (validacion.hasErrors()) {
            return VISTA;
        }
        return switch (registrarUsuario.registrar(form.aComando())) {
            case Result.Ok<UsuarioId, RegistroError> ok -> "redirect:/login?registrado";
            case Result.Err<UsuarioId, RegistroError> err -> {
                if (err.error() instanceof RegistroError.DominioCorreoNoPermitido dominio) {
                    validacion.rejectValue("correo", dominio.codigo(), dominio.mensaje());
                } else {
                    model.addAttribute("error", err.error().mensaje());
                }
                yield VISTA;
            }
        };
    }
}
