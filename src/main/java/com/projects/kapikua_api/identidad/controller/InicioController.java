package com.projects.kapikua_api.identidad.controller;

import com.projects.kapikua_api.identidad.security.SecurityUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class InicioController {
    /** Sin sesión se muestra la portada pública; con sesión, se entra directo a la billetera o al panel. */
    @GetMapping("/")
    String inicio(@AuthenticationPrincipal SecurityUser usuario) {
        if (usuario == null) return "identidad/portada";
        return "ADMINISTRADOR".equals(usuario.rol()) ? "redirect:/admin" : "redirect:/billetera";
    }

    @GetMapping("/admin")
    String panelAdmin(@AuthenticationPrincipal SecurityUser usuario, Model model) {
        model.addAttribute("usuario", usuario);
        return "identidad/inicio";
    }
}
