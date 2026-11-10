package com.projects.kapikua_api.cumplimiento.infrastructure.adapter.in.web;

import com.projects.kapikua_api.cumplimiento.application.port.in.ReclamosUseCase;
import com.projects.kapikua_api.cumplimiento.application.port.in.ReclamosUseCase.VistaReclamo;
import com.projects.kapikua_api.cumplimiento.domain.error.ErrorCumplimiento;
import com.projects.kapikua_api.cumplimiento.domain.model.MotivoReclamo;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/reclamos")
class ReclamosController {
    private final ReclamosUseCase reclamos;

    ReclamosController(ReclamosUseCase reclamos) {
        this.reclamos = reclamos;
    }

    @GetMapping
    String lista(@AuthenticationPrincipal UsuarioAutenticado u, Model model) {
        model.addAttribute("reclamos", reclamos.deUsuario(u.usuarioId()));
        return "cumplimiento/reclamos";
    }

    @GetMapping("/nuevo")
    String nuevo(@RequestParam(required = false) Long operacion, @RequestParam(required = false) String motivo, Model model) {
        model.addAttribute("motivos", MotivoReclamo.values());
        model.addAttribute("operacion", operacion);
        model.addAttribute("motivo", operacion != null && motivo == null ? MotivoReclamo.OPERACION.name() : motivo);
        return "cumplimiento/reclamo-nuevo";
    }

    @PostMapping
    String registrar(@AuthenticationPrincipal UsuarioAutenticado u, @RequestParam MotivoReclamo motivo,
                     @RequestParam(defaultValue = "") String detalle, @RequestParam(required = false) Long operacion,
                     Model model) {
        return switch (reclamos.registrar(u.usuarioId(), motivo, detalle, operacion)) {
            case Result.Ok<VistaReclamo, ErrorCumplimiento> ok -> "redirect:/reclamos/" + ok.valor().id() + "?registrado";
            case Result.Err<VistaReclamo, ErrorCumplimiento> err -> {
                nuevo(operacion, motivo.name(), model);
                model.addAttribute("detalle", detalle);
                model.addAttribute("error", err.error().mensaje());
                yield "cumplimiento/reclamo-nuevo";
            }
        };
    }

    @GetMapping("/{id}")
    String detalle(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long id,
                   @RequestParam(required = false) String registrado, Model model) {
        return switch (reclamos.delUsuario(u.usuarioId(), id)) {
            case Result.Ok<VistaReclamo, ErrorCumplimiento> ok -> {
                model.addAttribute("r", ok.valor());
                model.addAttribute("registrado", registrado != null);
                yield "cumplimiento/reclamo";
            }
            case Result.Err<VistaReclamo, ErrorCumplimiento> err -> "redirect:/reclamos";
        };
    }
}
