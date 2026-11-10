package com.projects.kapikua_api.credito.infrastructure.adapter.in.web;

import com.projects.kapikua_api.credito.application.port.in.PagarPrestamoUseCase;
import com.projects.kapikua_api.credito.application.port.in.PagarPrestamoUseCase.VistaAnticipado;
import com.projects.kapikua_api.credito.application.port.in.PagarPrestamoUseCase.VistaPago;
import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.credito.domain.model.ModalidadPago;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/** CU-11 Pagar cuota o adelantar pago (2 pantallas: confirmar y resultado en el préstamo) y RN-22 revocar el cargo. */
@Controller
@RequestMapping("/credito/prestamo")
class PagosController {

    private final PagarPrestamoUseCase pagos;

    PagosController(PagarPrestamoUseCase pagos) {
        this.pagos = pagos;
    }

    @GetMapping("/pagar")
    String pagarForm(@AuthenticationPrincipal UsuarioAutenticado u, Model model) {
        return switch (pagos.prepararPagoCuotas(u.usuarioId())) {
            case Result.Ok<VistaPago, ErrorCredito> ok -> {
                model.addAttribute("v", ok.valor());
                yield "credito/pagar";
            }
            case Result.Err<VistaPago, ErrorCredito> err -> "redirect:/credito/prestamo";
        };
    }

    @PostMapping("/pagar")
    String pagar(@AuthenticationPrincipal UsuarioAutenticado u, @RequestParam String referencia, Model model) {
        return switch (pagos.pagarCuotas(u.usuarioId(), referencia)) {
            case Result.Ok<Dinero, ErrorCredito> ok -> "redirect:/credito/prestamo?aviso=pagado";
            case Result.Err<Dinero, ErrorCredito> err -> {
                model.addAttribute("error", err.error().mensaje());
                yield pagarForm(u, model);
            }
        };
    }

    @GetMapping("/anticipado")
    String anticipadoForm(@AuthenticationPrincipal UsuarioAutenticado u, Model model) {
        return switch (pagos.prepararAnticipado(u.usuarioId())) {
            case Result.Ok<VistaAnticipado, ErrorCredito> ok -> {
                model.addAttribute("v", ok.valor());
                model.addAttribute("modalidades", ModalidadPago.values());
                yield "credito/anticipado";
            }
            case Result.Err<VistaAnticipado, ErrorCredito> err -> "redirect:/credito/prestamo";
        };
    }

    @PostMapping("/anticipado")
    String anticipado(@AuthenticationPrincipal UsuarioAutenticado u, @RequestParam ModalidadPago modalidad,
                      @RequestParam(required = false) BigDecimal monto, @RequestParam String referencia, Model model) {
        var montoPago = monto == null || monto.signum() <= 0 || monto.scale() > 2 ? Dinero.CERO : Dinero.de(monto);
        return switch (pagos.pagarAnticipado(u.usuarioId(), modalidad, montoPago, referencia)) {
            case Result.Ok<Dinero, ErrorCredito> ok -> modalidad == ModalidadPago.TOTAL
                    ? "redirect:/credito?aviso=pagado-total" : "redirect:/credito/prestamo?aviso=anticipado";
            case Result.Err<Dinero, ErrorCredito> err -> {
                model.addAttribute("error", err.error().mensaje());
                yield anticipadoForm(u, model);
            }
        };
    }

    @PostMapping("/revocar-cargo")
    String revocarCargo(@AuthenticationPrincipal UsuarioAutenticado u) {
        pagos.revocarCargo(u.usuarioId());
        return "redirect:/credito/prestamo?aviso=cargo-revocado";
    }
}
