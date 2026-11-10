package com.projects.kapikua_api.evaluacion.infrastructure.adapter.in.web;

import com.projects.kapikua_api.evaluacion.application.port.in.ConsultarEvaluacionUseCase;
import com.projects.kapikua_api.evaluacion.application.port.in.ConsultarEvaluacionUseCase.NivelCliente;
import com.projects.kapikua_api.evaluacion.domain.error.ErrorEvaluacion;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** "Mi nivel": la escalera de créditos del cliente (RN-14, RN-26) y su avance hacia el uso mínimo (RN-08). */
@Controller
class NivelController {

    private final ConsultarEvaluacionUseCase consultar;

    NivelController(ConsultarEvaluacionUseCase consultar) {
        this.consultar = consultar;
    }

    @GetMapping("/nivel")
    String nivel(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        switch (consultar.nivel(usuario.usuarioId())) {
            case Result.Ok<NivelCliente, ErrorEvaluacion> ok -> model.addAttribute("n", ok.valor());
            case Result.Err<NivelCliente, ErrorEvaluacion> err -> model.addAttribute("error", err.error().mensaje());
        }
        return "evaluacion/nivel";
    }
}
