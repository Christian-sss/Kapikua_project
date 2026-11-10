package com.projects.kapikua_api.identidad.infrastructure.adapter.in.web;

import com.projects.kapikua_api.identidad.infrastructure.security.SecurityUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class InicioController {
    @GetMapping("/")
    String inicio(@AuthenticationPrincipal SecurityUser usuario) {
        return "ADMINISTRADOR".equals(usuario.rol()) ? "redirect:/admin" : "redirect:/billetera";
    }

    @GetMapping("/admin")
    String panelAdmin(@AuthenticationPrincipal SecurityUser usuario, Model model) {
        model.addAttribute("usuario", usuario);
        return "identidad/inicio";
    }
}
