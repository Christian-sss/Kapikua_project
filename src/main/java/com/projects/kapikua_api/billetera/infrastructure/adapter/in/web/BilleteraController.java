package com.projects.kapikua_api.billetera.infrastructure.adapter.in.web;

import com.projects.kapikua_api.billetera.application.port.in.ConsultarBilleteraUseCase;
import com.projects.kapikua_api.billetera.application.port.in.ConsultarBilleteraUseCase.ResumenBilletera;
import com.projects.kapikua_api.billetera.application.port.out.TransaccionRepositoryPort.LineaMovimiento;
import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.billetera.domain.model.TipoTransaccion;
import com.projects.kapikua_api.shared.UsuarioAutenticado;
import com.projects.kapikua_api.shared.Result;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** CU-03 Consultar billetera. */
@Controller
class BilleteraController {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ConsultarBilleteraUseCase consultar;
    private final Clock clock;

    BilleteraController(ConsultarBilleteraUseCase consultar, Clock clock) {
        this.consultar = consultar;
        this.clock = clock;
    }

    @GetMapping("/billetera")
    String inicio(@AuthenticationPrincipal UsuarioAutenticado usuario,
                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                  @RequestParam(required = false) Long operacion,
                  Model model) {
        model.addAttribute("usuario", usuario);
        // Comprobante de la operación recién hecha: solo si el usuario participó en ella.
        if (operacion != null) {
            consultar.comprobante(usuario.usuarioId(), operacion).ifPresent(c -> model.addAttribute("comprobante", c));
        }
        return poblar(usuario, desde, hasta, model) ? "billetera/inicio" : "billetera/error";
    }

    /**
     * RNF-17: la pantalla pide este fragmento al recibir un aviso SSE y lo reemplaza sin recargar.
     * No renueva la sesión (ver JwtCookieFilter). Si algo falla, redirige y la pantalla se recarga completa.
     */
    @GetMapping("/billetera/fragmentos/contenido")
    String fragmento(@AuthenticationPrincipal UsuarioAutenticado usuario,
                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                     Model model) {
        return poblar(usuario, desde, hasta, model) ? "billetera/inicio :: contenido" : "redirect:/billetera";
    }

    /** Saldo, límites y movimientos. Guarda el filtro tal como llegó para que el fragmento use el mismo. */
    private boolean poblar(UsuarioAutenticado usuario, LocalDate desde, LocalDate hasta, Model model) {
        model.addAttribute("filtroDesde", desde);
        model.addAttribute("filtroHasta", hasta);
        return switch (consultar.resumen(usuario.usuarioId(), desde, hasta)) {
            case Result.Ok<ResumenBilletera, ErrorBilletera> ok -> {
                model.addAttribute("b", ok.valor());
                model.addAttribute("movimientos", ok.valor().movimientos().stream().map(this::vista).toList());
                yield true;
            }
            case Result.Err<ResumenBilletera, ErrorBilletera> err -> {
                model.addAttribute("error", err.error().mensaje());
                yield false;
            }
        };
    }

    /** Un movimiento listo para la pantalla, con la fecha en hora de Lima. */
    record MovimientoVista(String fecha, String etiqueta, String detalle, String monto, boolean entrada, String saldo) {}

    private MovimientoVista vista(LineaMovimiento m) {
        var fecha = m.fecha().atZoneSameInstant(clock.getZone()).format(FECHA);
        return new MovimientoVista(fecha, etiqueta(m.tipo(), m.esEntrada()), m.descripcion(),
                (m.esEntrada() ? "+ " : "− ") + m.montoAbsoluto(), m.esEntrada(),
                "S/ " + m.saldoPosterior().toPlainString());
    }

    static String etiqueta(TipoTransaccion tipo, boolean entrada) {
        return switch (tipo) {
            case RECARGA -> "Recarga con tarjeta";
            case TRANSFERENCIA -> entrada ? "Transferencia recibida" : "Transferencia enviada";
            case PAGO_NEGOCIO -> entrada ? "Cobro de un cliente" : "Pago a negocio";
            case DESEMBOLSO -> "Desembolso de crédito";
            case PAGO_CUOTA -> "Pago de cuota";
            case PAGO_ANTICIPADO -> "Pago anticipado";
        };
    }
}
