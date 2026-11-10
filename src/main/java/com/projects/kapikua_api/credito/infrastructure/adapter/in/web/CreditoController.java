package com.projects.kapikua_api.credito.infrastructure.adapter.in.web;

import com.projects.kapikua_api.credito.application.port.in.AceptarOfertaUseCase;
import com.projects.kapikua_api.credito.application.port.in.ConsultarCreditoUseCase;
import com.projects.kapikua_api.credito.application.port.in.GestionarAvalUseCase;
import com.projects.kapikua_api.credito.application.port.in.SimularCreditoUseCase;
import com.projects.kapikua_api.credito.application.port.in.SolicitarCreditoUseCase;
import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.evaluacion.domain.model.Destino;
import com.projects.kapikua_api.evaluacion.domain.model.Recomendacion;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * Pantallas de crédito (CU-07 a CU-10, CU-12). Sin atributos flash (la app no usa sesión): ante un error se
 * vuelve a mostrar la misma pantalla con el mensaje; ante un éxito se redirige.
 */
@Controller
@RequestMapping("/credito")
class CreditoController {

    private final ConsultarCreditoUseCase consultar;
    private final SimularCreditoUseCase simular;
    private final SolicitarCreditoUseCase solicitar;
    private final GestionarAvalUseCase avales;
    private final AceptarOfertaUseCase aceptar;

    CreditoController(ConsultarCreditoUseCase consultar, SimularCreditoUseCase simular, SolicitarCreditoUseCase solicitar,
                      GestionarAvalUseCase avales, AceptarOfertaUseCase aceptar) {
        this.consultar = consultar;
        this.simular = simular;
        this.solicitar = solicitar;
        this.avales = avales;
        this.aceptar = aceptar;
    }

    // ---------------------------------------------------------------- panel y préstamo (CU-12)

    @GetMapping
    String panel(@AuthenticationPrincipal UsuarioAutenticado u, @RequestParam(required = false) String aviso, Model model) {
        model.addAttribute("panel", consultar.panel(u.usuarioId()));
        model.addAttribute("aviso", aviso);
        return "credito/panel";
    }

    @GetMapping("/prestamo")
    String prestamo(@AuthenticationPrincipal UsuarioAutenticado u, @RequestParam(required = false) String aviso,
                    Model model) {
        var p = consultar.panel(u.usuarioId()).prestamo();
        if (p == null) return "redirect:/credito";
        model.addAttribute("p", p);
        model.addAttribute("aviso", aviso);
        return "credito/prestamo";
    }

    // ---------------------------------------------------------------- simular (CU-07)

    @GetMapping("/simular")
    String simularForm(@AuthenticationPrincipal UsuarioAutenticado u, Model model) {
        resultado(simular.opciones(u.usuarioId()), "opciones", model);
        return "credito/simular";
    }

    @PostMapping("/simular")
    String simular(@AuthenticationPrincipal UsuarioAutenticado u, @RequestParam BigDecimal monto,
                   @RequestParam int cuotas, Model model) {
        resultado(simular.opciones(u.usuarioId()), "opciones", model);
        model.addAttribute("monto", monto);
        model.addAttribute("cuotas", cuotas);
        if (monto.signum() <= 0 || monto.scale() > 2) {
            model.addAttribute("error", new ErrorCredito.MontoInvalido().mensaje());
        } else {
            resultado(simular.simular(u.usuarioId(), Dinero.de(monto), cuotas), "s", model);
        }
        return "credito/simular";
    }

    // ---------------------------------------------------------------- solicitar (CU-08)

    @GetMapping("/solicitar")
    String solicitarForm(@AuthenticationPrincipal UsuarioAutenticado u,
                         @RequestParam(required = false) BigDecimal monto, @RequestParam(required = false) Integer cuotas,
                         Model model) {
        resultado(simular.opciones(u.usuarioId()), "opciones", model);
        model.addAttribute("monto", monto);
        model.addAttribute("cuotas", cuotas);
        model.addAttribute("destinos", Destino.values());
        return "credito/solicitar";
    }

    @PostMapping("/solicitar")
    String solicitar(@AuthenticationPrincipal UsuarioAutenticado u, @RequestParam BigDecimal monto,
                     @RequestParam int cuotas, @RequestParam Destino destino,
                     @RequestParam(defaultValue = "false") boolean autorizaDatos, Model model) {
        if (monto.signum() <= 0 || monto.scale() > 2) {
            return conError(solicitarForm(u, monto, cuotas, model), new ErrorCredito.MontoInvalido(), model);
        }
        return switch (solicitar.solicitar(u.usuarioId(), Dinero.de(monto), cuotas, destino, autorizaDatos)) {
            case Result.Ok<SolicitarCreditoUseCase.ResultadoSolicitud, ErrorCredito> ok -> {
                if (ok.valor().recomendacion() == Recomendacion.NO_APTO) {
                    model.addAttribute("r", ok.valor());
                    yield "credito/no-apto";
                }
                yield "redirect:/credito/ofertas/" + ok.valor().ofertaId();
            }
            case Result.Err<SolicitarCreditoUseCase.ResultadoSolicitud, ErrorCredito> err -> {
                solicitarForm(u, monto, cuotas, model);
                yield conError("credito/solicitar", err.error(), model);
            }
        };
    }

    // ---------------------------------------------------------------- oferta, aval del deudor y entidad (CU-08, CU-09)

    @GetMapping("/ofertas/{id}")
    String oferta(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long id,
                  @RequestParam(required = false) String aviso, Model model) {
        model.addAttribute("aviso", aviso);
        return switch (consultar.oferta(u.usuarioId(), id)) {
            case Result.Ok<ConsultarCreditoUseCase.DetalleOferta, ErrorCredito> ok -> {
                model.addAttribute("o", ok.valor());
                yield "credito/oferta";
            }
            case Result.Err<ConsultarCreditoUseCase.DetalleOferta, ErrorCredito> err -> "redirect:/credito";
        };
    }

    @PostMapping("/ofertas/{id}/aval")
    String designarAval(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long id,
                        @RequestParam String celular, Model model) {
        return switch (avales.designar(u.usuarioId(), id, celular)) {
            case Result.Ok<Void, ErrorCredito> ok -> "redirect:/credito/ofertas/" + id + "?aviso=aval";
            case Result.Err<Void, ErrorCredito> err -> conError(oferta(u, id, null, model), err.error(), model);
        };
    }

    @PostMapping("/ofertas/{id}/enviar")
    String enviarAEntidad(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long id, Model model) {
        return switch (aceptar.enviarAEntidad(u.usuarioId(), id)) {
            case Result.Ok<Void, ErrorCredito> ok -> "redirect:/credito/ofertas/" + id;
            case Result.Err<Void, ErrorCredito> err -> conError(oferta(u, id, null, model), err.error(), model);
        };
    }

    // ---------------------------------------------------------------- hoja resumen y aceptación (CU-10)

    @GetMapping("/ofertas/{id}/hoja")
    String hoja(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long id, Model model) {
        return switch (aceptar.hojaResumen(u.usuarioId(), id)) {
            case Result.Ok<AceptarOfertaUseCase.HojaResumen, ErrorCredito> ok -> {
                model.addAttribute("h", ok.valor());
                yield "credito/hoja";
            }
            case Result.Err<AceptarOfertaUseCase.HojaResumen, ErrorCredito> err -> conError(oferta(u, id, null, model), err.error(), model);
        };
    }

    @PostMapping("/ofertas/{id}/aceptar")
    String aceptar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long id,
                   @RequestParam(defaultValue = "") String codigo,
                   @RequestParam(defaultValue = "false") boolean autorizaCargo,
                   HttpServletRequest request, Model model) {
        return switch (aceptar.aceptar(u.usuarioId(), id, codigo, request.getHeader(HttpHeaders.USER_AGENT), autorizaCargo)) {
            case Result.Ok<Long, ErrorCredito> ok -> "redirect:/credito/prestamo";
            // Con un código fallido se pide una hoja nueva (y un código nuevo).
            case Result.Err<Long, ErrorCredito> err -> conError(hoja(u, id, model), err.error(), model);
        };
    }

    // ---------------------------------------------------------------- el aval responde (CU-09)

    @GetMapping("/aval/{ofertaId}")
    String invitacion(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long ofertaId, Model model) {
        return switch (avales.invitacion(u.usuarioId(), ofertaId)) {
            case Result.Ok<GestionarAvalUseCase.InvitacionAval, ErrorCredito> ok -> {
                model.addAttribute("i", ok.valor());
                yield "credito/aval";
            }
            case Result.Err<GestionarAvalUseCase.InvitacionAval, ErrorCredito> err -> "redirect:/credito";
        };
    }

    @PostMapping("/aval/{ofertaId}")
    String responder(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long ofertaId,
                     @RequestParam boolean acepta, @RequestParam(defaultValue = "") String codigo,
                     @RequestParam(defaultValue = "false") boolean autorizaGarantia,
                     HttpServletRequest request, Model model) {
        // RN-07: el aval autoriza por separado el uso de sus datos y la retención de sus ventas.
        if (acepta && !autorizaGarantia) {
            return conError(invitacion(u, ofertaId, model), new ErrorCredito.GarantiaNoAutorizada(), model);
        }
        return switch (avales.responder(u.usuarioId(), ofertaId, acepta, codigo, request.getHeader(HttpHeaders.USER_AGENT))) {
            case Result.Ok<Void, ErrorCredito> ok -> "redirect:/credito?aviso=" + (acepta ? "aval-aceptado" : "aval-rechazado");
            case Result.Err<Void, ErrorCredito> err -> conError(invitacion(u, ofertaId, model), err.error(), model);
        };
    }

    // ---------------------------------------------------------------- apoyo

    private static <T> void resultado(Result<T, ErrorCredito> r, String nombre, Model model) {
        switch (r) {
            case Result.Ok<T, ErrorCredito> ok -> model.addAttribute(nombre, ok.valor());
            case Result.Err<T, ErrorCredito> err -> model.addAttribute("error", err.error().mensaje());
        }
    }

    private static String conError(String vista, ErrorCredito error, Model model) {
        model.addAttribute("error", error.mensaje());
        return vista;
    }
}
