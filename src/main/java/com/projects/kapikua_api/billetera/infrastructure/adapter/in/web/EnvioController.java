package com.projects.kapikua_api.billetera.infrastructure.adapter.in.web;

import com.projects.kapikua_api.billetera.application.port.in.Comprobante;
import com.projects.kapikua_api.billetera.application.port.in.EnviarDineroUseCase;
import com.projects.kapikua_api.billetera.application.port.in.EnviarDineroUseCase.VistaPrevia;
import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.UsuarioAutenticado;
import com.projects.kapikua_api.shared.Result;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * CU-05 Transferir por celular y CU-06 Pagar a un negocio, en 3 pantallas (RNF-13):
 * datos → confirmación → comprobante (en la billetera).
 */
@Controller
@RequestMapping("/billetera")
class EnvioController {

    private final EnviarDineroUseCase enviar;

    EnvioController(EnviarDineroUseCase enviar) {
        this.enviar = enviar;
    }

    // ---------------------------------------------------------------- transferencia

    @GetMapping("/transferir")
    String transferir(Model model) {
        model.addAttribute("form", new EnvioForm());
        model.addAttribute("modo", Modo.TRANSFERENCIA);
        return "billetera/envio";
    }

    @PostMapping("/transferir")
    String prepararTransferencia(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                 @Valid @ModelAttribute("form") EnvioForm form, BindingResult validacion, Model model) {
        model.addAttribute("modo", Modo.TRANSFERENCIA);
        if (validacion.hasErrors()) return "billetera/envio";
        return vistaPrevia(enviar.prepararTransferencia(usuario.usuarioId(), form.getCelular(), form.dinero(),
                form.getReferencia()), model);
    }

    @PostMapping("/transferir/confirmar")
    String confirmarTransferencia(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                  @Valid @ModelAttribute("form") EnvioForm form, BindingResult validacion,
                                  Model model) {
        model.addAttribute("modo", Modo.TRANSFERENCIA);
        if (validacion.hasErrors()) return "billetera/envio";
        return terminar(enviar.transferir(usuario.usuarioId(), form.getCelular(), form.dinero(),
                form.getReferencia(), form.getCodigo()), form, model);
    }

    // ---------------------------------------------------------------- pago a negocio

    @GetMapping("/pagar")
    String pagar(Model model) {
        model.addAttribute("form", new EnvioForm());
        model.addAttribute("modo", Modo.PAGO_NEGOCIO);
        return "billetera/envio";
    }

    @PostMapping("/pagar")
    String prepararPago(@AuthenticationPrincipal UsuarioAutenticado usuario,
                        @Valid @ModelAttribute("form") EnvioForm form, BindingResult validacion, Model model) {
        model.addAttribute("modo", Modo.PAGO_NEGOCIO);
        if (validacion.hasErrors()) return "billetera/envio";
        return vistaPrevia(enviar.prepararPagoNegocio(usuario.usuarioId(), form.getCelular(), form.dinero(),
                form.getReferencia()), model);
    }

    @PostMapping("/pagar/confirmar")
    String confirmarPago(@AuthenticationPrincipal UsuarioAutenticado usuario,
                         @Valid @ModelAttribute("form") EnvioForm form, BindingResult validacion,
                         Model model) {
        model.addAttribute("modo", Modo.PAGO_NEGOCIO);
        if (validacion.hasErrors()) return "billetera/envio";
        return terminar(enviar.pagarNegocio(usuario.usuarioId(), form.getCelular(), form.dinero(),
                form.getReferencia()), form, model);
    }

    // ---------------------------------------------------------------- común

    /** Textos y rutas de cada operación, para usar una sola plantilla. */
    enum Modo {
        TRANSFERENCIA("Transferir", "/billetera/transferir", "Celular de destino", true),
        PAGO_NEGOCIO("Pagar a un negocio", "/billetera/pagar", "Celular del negocio", false);

        public final String titulo;
        public final String ruta;
        public final String etiquetaCelular;
        public final boolean pideCodigo;

        Modo(String titulo, String ruta, String etiquetaCelular, boolean pideCodigo) {
            this.titulo = titulo;
            this.ruta = ruta;
            this.etiquetaCelular = etiquetaCelular;
            this.pideCodigo = pideCodigo;
        }
    }

    private String vistaPrevia(Result<VistaPrevia, ErrorBilletera> resultado, Model model) {
        return switch (resultado) {
            case Result.Ok<VistaPrevia, ErrorBilletera> ok -> {
                model.addAttribute("vista", ok.valor());
                yield "billetera/envio-confirmar";
            }
            case Result.Err<VistaPrevia, ErrorBilletera> err -> {
                model.addAttribute("error", err.error().mensaje());
                yield "billetera/envio";
            }
        };
    }

    /** Sin atributos flash (la app no usa HttpSession): la billetera carga el comprobante por su id. */
    private String terminar(Result<Comprobante, ErrorBilletera> resultado, EnvioForm form, Model model) {
        return switch (resultado) {
            case Result.Ok<Comprobante, ErrorBilletera> ok -> "redirect:/billetera?operacion=" + ok.valor().transaccionId();
            case Result.Err<Comprobante, ErrorBilletera> err -> {
                // Con la referencia nueva, el cliente vuelve a preparar la operación (y pide otro código).
                if (err.error() instanceof ErrorBilletera.ReferenciaInvalida) form.nuevaReferencia();
                form.setCodigo(null);
                model.addAttribute("error", err.error().mensaje());
                yield "billetera/envio";
            }
        };
    }
}
