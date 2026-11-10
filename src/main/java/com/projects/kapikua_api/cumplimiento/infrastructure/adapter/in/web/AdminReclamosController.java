package com.projects.kapikua_api.cumplimiento.infrastructure.adapter.in.web;

import com.projects.kapikua_api.cumplimiento.application.port.in.ReclamosUseCase;
import com.projects.kapikua_api.cumplimiento.application.port.in.ReclamosUseCase.VistaReclamo;
import com.projects.kapikua_api.cumplimiento.domain.error.ErrorCumplimiento;
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
@RequestMapping("/admin/reclamos")
class AdminReclamosController {
    private final ReclamosUseCase reclamos;

    AdminReclamosController(ReclamosUseCase reclamos) {
        this.reclamos = reclamos;
    }

    @GetMapping
    String lista(@RequestParam(required = false) String aviso, Model model) {
        model.addAttribute("pendientes", reclamos.pendientes());
        model.addAttribute("atendidos", reclamos.atendidos(20));
        model.addAttribute("aviso", aviso);
        return "cumplimiento/admin-reclamos";
    }

    @GetMapping("/{id}")
    String detalle(@PathVariable Long id, @RequestParam(required = false) String aviso, Model model) {
        model.addAttribute("aviso", aviso);
        return switch (reclamos.detalle(id)) {
            case Result.Ok<VistaReclamo, ErrorCumplimiento> ok -> {
                model.addAttribute("r", ok.valor());
                yield "cumplimiento/admin-reclamo";
            }
            case Result.Err<VistaReclamo, ErrorCumplimiento> err -> "redirect:/admin/reclamos";
        };
    }

    @PostMapping("/{id}/respuesta")
    String responder(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long id,
                     @RequestParam(defaultValue = "") String respuesta, Model model) {
        return switch (reclamos.responder(id, u.usuarioId(), respuesta)) {
            case Result.Ok<Void, ErrorCumplimiento> ok -> "redirect:/admin/reclamos?aviso=respondido";
            case Result.Err<Void, ErrorCumplimiento> err -> {
                model.addAttribute("error", err.error().mensaje());
                model.addAttribute("respuesta", respuesta);
                yield detalle(id, null, model);
            }
        };
    }

    @PostMapping("/{id}/prorroga")
    String prorrogar(@PathVariable Long id, @RequestParam(defaultValue = "") String justificacion, Model model) {
        return switch (reclamos.prorrogar(id, justificacion)) {
            case Result.Ok<Void, ErrorCumplimiento> ok -> "redirect:/admin/reclamos/" + id + "?aviso=prorroga";
            case Result.Err<Void, ErrorCumplimiento> err -> {
                model.addAttribute("error", err.error().mensaje());
                yield detalle(id, null, model);
            }
        };
    }
}
