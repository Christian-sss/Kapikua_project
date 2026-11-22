package com.projects.kapikua_api.credito.controller;

import com.projects.kapikua_api.credito.service.AceptacionService;
import com.projects.kapikua_api.credito.service.ConsultaCreditoService;
import com.projects.kapikua_api.credito.service.AvalService;
import com.projects.kapikua_api.credito.service.OriginacionService;
import com.projects.kapikua_api.credito.error.ErrorCredito;
import com.projects.kapikua_api.credito.dto.DetalleOferta;
import com.projects.kapikua_api.credito.dto.HojaResumen;
import com.projects.kapikua_api.credito.dto.InvitacionAval;
import com.projects.kapikua_api.credito.dto.ResultadoSolicitud;
import com.projects.kapikua_api.credito.dto.SolicitudCreditoForm;
import com.projects.kapikua_api.evaluacion.domain.model.Destino;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
@RequestMapping("/credito")
class CreditoController {

    private final ConsultaCreditoService consultar;
    private final OriginacionService originacion;
    private final AvalService avales;
    private final AceptacionService aceptar;

    CreditoController(ConsultaCreditoService consultar, OriginacionService originacion, AvalService avales,
                      AceptacionService aceptar) {
        this.consultar = consultar;
        this.originacion = originacion;
        this.avales = avales;
        this.aceptar = aceptar;
    }

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

    @GetMapping("/simular")
    String simularForm(@AuthenticationPrincipal UsuarioAutenticado u, Model model) {
        resultado(originacion.opciones(u.usuarioId()), "opciones", model);
        return "credito/simular";
    }

    @PostMapping("/simular")
    String simular(@AuthenticationPrincipal UsuarioAutenticado u, @RequestParam BigDecimal monto,
                   @RequestParam int cuotas, Model model) {
        resultado(originacion.opciones(u.usuarioId()), "opciones", model);
        model.addAttribute("monto", monto);
        model.addAttribute("cuotas", cuotas);
        if (monto.signum() <= 0 || monto.scale() > 2) {
            model.addAttribute("error", new ErrorCredito.MontoInvalido().mensaje());
        } else {
            resultado(originacion.simular(u.usuarioId(), Dinero.de(monto), cuotas), "s", model);
        }
        return "credito/simular";
    }

    @GetMapping("/solicitar")
    String solicitarForm(@AuthenticationPrincipal UsuarioAutenticado u,
                         @RequestParam(required = false) BigDecimal monto, @RequestParam(required = false) Integer cuotas,
                         Model model) {
        resultado(originacion.opciones(u.usuarioId()), "opciones", model);
        model.addAttribute("monto", monto);
        model.addAttribute("cuotas", cuotas);
        model.addAttribute("destinos", Destino.values());
        return "credito/solicitar";
    }

    @PostMapping("/solicitar")
    String solicitar(
            @AuthenticationPrincipal UsuarioAutenticado u,
            @Valid @ModelAttribute("form") SolicitudCreditoForm form,
            BindingResult errores,
            Model model
    ) {

            if(errores.hasErrors())
                return solicitarForm(u,form.monto(),form.cuotas(),model);


        return switch (originacion.solicitar(
                u.usuarioId(),
                form.dinero(),
                form.cuotas(),
                form.destino(),
                form.autorizaDatos()
        )) {
            case Result.Ok<ResultadoSolicitud, ErrorCredito> ok ->
                vistaDe(ok.valor(),model);
            case Result.Err<ResultadoSolicitud, ErrorCredito> err ->
                conError(solicitarForm(u,form.monto(), form.cuotas(),model),err.error(),model);

        };
    }

    private static String vistaDe(ResultadoSolicitud r, Model model) {
        if (r.tieneOferta()) return "redirect:/credito/ofertas/" + r.ofertaId();
        model.addAttribute("r", r);
        return "credito/no-apto";
    }

    @GetMapping("/ofertas/{id}")
    String oferta(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long id,
                  @RequestParam(required = false) String aviso, Model model) {
        model.addAttribute("aviso", aviso);
        return switch (consultar.oferta(u.usuarioId(), id)) {
            case Result.Ok<DetalleOferta, ErrorCredito> ok -> {
                model.addAttribute("o", ok.valor());
                yield "credito/oferta";
            }
            case Result.Err<DetalleOferta, ErrorCredito> err -> "redirect:/credito";
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

    @GetMapping("/ofertas/{id}/hoja")
    String hoja(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long id, Model model) {
        return switch (aceptar.hojaResumen(u.usuarioId(), id)) {
            case Result.Ok<HojaResumen, ErrorCredito> ok -> {
                model.addAttribute("h", ok.valor());
                yield "credito/hoja";
            }
            case Result.Err<HojaResumen, ErrorCredito> err ->
                    conError(oferta(u, id, null, model), err.error(), model);
        };
    }

    @PostMapping("/ofertas/{id}/aceptar")
    String aceptar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long id,
                   @RequestParam(defaultValue = "") String codigo,
                   @RequestParam(defaultValue = "false") boolean autorizaCargo,
                   HttpServletRequest request, Model model) {
        return switch (aceptar.aceptar(u.usuarioId(), id, codigo, request.getHeader(HttpHeaders.USER_AGENT), autorizaCargo)) {
            case Result.Ok<Long, ErrorCredito> ok -> "redirect:/credito/prestamo";
            case Result.Err<Long, ErrorCredito> err -> conError(hoja(u, id, model), err.error(), model);
        };
    }

    @GetMapping("/aval/{ofertaId}")
    String invitacion(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long ofertaId, Model model) {
        return switch (avales.invitacion(u.usuarioId(), ofertaId)) {
            case Result.Ok<InvitacionAval, ErrorCredito> ok -> {
                model.addAttribute("i", ok.valor());
                yield "credito/aval";
            }
            case Result.Err<InvitacionAval, ErrorCredito> err -> "redirect:/credito";
        };
    }

    @PostMapping("/aval/{ofertaId}")
    String responder(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long ofertaId,
                     @RequestParam boolean acepta, @RequestParam(defaultValue = "") String codigo,
                     @RequestParam(defaultValue = "false") boolean autorizaGarantia,
                     HttpServletRequest request, Model model) {
        if (acepta && !autorizaGarantia) {
            return conError(invitacion(u, ofertaId, model), new ErrorCredito.GarantiaNoAutorizada(), model);
        }
        return switch (avales.responder(u.usuarioId(), ofertaId, acepta, codigo, request.getHeader(HttpHeaders.USER_AGENT))) {
            case Result.Ok<Void, ErrorCredito> ok ->
                    "redirect:/credito?aviso=" + (acepta ? "aval-aceptado" : "aval-rechazado");
            case Result.Err<Void, ErrorCredito> err -> conError(invitacion(u, ofertaId, model), err.error(), model);
        };
    }

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
