package com.projects.kapikua_api.cumplimiento.infrastructure.adapter.in.web;

import com.projects.kapikua_api.cumplimiento.application.port.in.RevisarAlertasUseCase;
import com.projects.kapikua_api.cumplimiento.application.port.in.RevisarAlertasUseCase.DetalleAlerta;
import com.projects.kapikua_api.cumplimiento.domain.error.ErrorCumplimiento;
import com.projects.kapikua_api.cumplimiento.domain.model.EstadoAlerta;
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
@RequestMapping("/admin/alertas")
class AdminAlertasController {
    private final RevisarAlertasUseCase alertas;

    AdminAlertasController(RevisarAlertasUseCase alertas) {
        this.alertas = alertas;
    }

    @GetMapping
    String lista(@RequestParam(required = false) String aviso, Model model) {
        model.addAttribute("pendientes", alertas.pendientes());
        model.addAttribute("revisadas", alertas.revisadas(20));
        model.addAttribute("aviso", aviso);
        return "cumplimiento/admin-alertas";
    }

    @GetMapping("/{id}")
    String detalle(@PathVariable Long id, Model model) {
        return switch (alertas.detalle(id)) {
            case Result.Ok<DetalleAlerta, ErrorCumplimiento> ok -> {
                model.addAttribute("d", ok.valor());
                yield "cumplimiento/admin-alerta";
            }
            case Result.Err<DetalleAlerta, ErrorCumplimiento> err -> "redirect:/admin/alertas";
        };
    }

    @PostMapping("/{id}/revision")
    String revisar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long id,
                   @RequestParam EstadoAlerta decision, @RequestParam(defaultValue = "") String comentario,
                   @RequestParam(defaultValue = "false") boolean bloquear,
                   @RequestParam(defaultValue = "") String motivoBloqueo, Model model) {
        return switch (alertas.revisar(id, u.usuarioId(), decision, comentario, bloquear, motivoBloqueo)) {
            case Result.Ok<Void, ErrorCumplimiento> ok -> "redirect:/admin/alertas?aviso=revisada";
            case Result.Err<Void, ErrorCumplimiento> err -> {
                model.addAttribute("error", err.error().mensaje());
                model.addAttribute("decision", decision.name());
                model.addAttribute("comentario", comentario);
                model.addAttribute("bloquear", bloquear);
                model.addAttribute("motivoBloqueo", motivoBloqueo);
                yield detalle(id, model);
            }
        };
    }
}
