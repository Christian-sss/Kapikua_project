package com.projects.kapikua_api.identidad.controller;

import com.projects.kapikua_api.identidad.service.RegistroService;
import com.projects.kapikua_api.identidad.error.RegistroError;
import com.projects.kapikua_api.identidad.dto.RegistroForm;
import com.projects.kapikua_api.identidad.entity.DatosRegistroUsuario;
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

    private final RegistroService registroService;

    public RegistroController(RegistroService registroService) {
        this.registroService = registroService;
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
        return switch (registroService.registrar(form)) {
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
