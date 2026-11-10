package com.projects.kapikua_api.billetera.infrastructure.adapter.in.web;

import com.projects.kapikua_api.billetera.application.port.in.Comprobante;
import com.projects.kapikua_api.billetera.application.port.in.RecargarSaldoUseCase;
import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.UsuarioAutenticado;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;
import java.util.UUID;

@Controller
class RecargaController {
    private final RecargarSaldoUseCase recargar;

    RecargaController(RecargarSaldoUseCase recargar) {
        this.recargar = recargar;
    }

    @Getter
    @Setter
    public static class RecargaForm {
        @NotNull(message = "Ingresa el monto")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero")
        @Digits(integer = 7, fraction = 2, message = "Usa hasta 2 decimales")
        private BigDecimal monto;

        @NotBlank(message = "Elige una tarjeta")
        private String token = "tok_aprobado";

        private String referencia = UUID.randomUUID().toString();
    }

    @GetMapping("/billetera/recargar")
    String formulario(Model model) {
        model.addAttribute("form", new RecargaForm());
        return "billetera/recarga";
    }

    @PostMapping("/billetera/recargar")
    String recargar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                    @Valid @ModelAttribute("form") RecargaForm form, BindingResult validacion, Model model) {
        if (validacion.hasErrors()) return "billetera/recarga";
        return switch (recargar.recargar(usuario.usuarioId(), Dinero.de(form.getMonto()), form.getToken(),
                form.getReferencia())) {
            case Result.Ok<Comprobante, ErrorBilletera> ok -> "redirect:/billetera?operacion=" + ok.valor().transaccionId();
            case Result.Err<Comprobante, ErrorBilletera> err -> {
                model.addAttribute("error", err.error().mensaje());
                yield "billetera/recarga";
            }
        };
    }
}
