package com.projects.kapikua_api.administracion;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;

/** CU-16 Consultar reportes (/admin/** exige el rol ADMINISTRADOR; un Cliente recibe 403, RNF-05). Solo GET. */
@Controller
@RequestMapping("/admin/reportes")
class ReportesController {

    private final ReportesConsulta consulta;
    private final Clock clock;

    ReportesController(ReportesConsulta consulta, Clock clock) {
        this.consulta = consulta;
        this.clock = clock;
    }

    @GetMapping
    String inicio() {
        return "redirect:/admin/reportes/clientes";
    }

    @GetMapping("/{tipo}")
    String reporte(@PathVariable String tipo, @RequestParam(required = false) String dia,
                   @RequestParam(required = false) String mes, @RequestParam(required = false) String estado, Model model) {
        var reporte = Arrays.stream(TipoReporte.values()).filter(t -> t.ruta().equals(tipo)).findFirst();
        if (reporte.isEmpty()) return "redirect:/admin/reportes/clientes";
        var t = reporte.get();
        var periodo = Periodo.de(dia, mes, LocalDate.now(clock));
        var filtro = t.estadoValido(estado);
        model.addAttribute("tipo", t);
        model.addAttribute("tipos", TipoReporte.values());
        model.addAttribute("periodo", periodo);
        model.addAttribute("estado", filtro);
        model.addAttribute("r", switch (t) {
            case CLIENTES -> consulta.clientes(periodo, filtro);
            case TRANSACCIONES -> consulta.transacciones(periodo, filtro);
            case CREDITOS -> consulta.creditos(periodo, filtro);
            case COBRANZA -> consulta.cobranza(periodo, filtro);
        });
        return "administracion/reportes";
    }
}
