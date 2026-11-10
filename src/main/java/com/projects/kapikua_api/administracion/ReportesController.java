package com.projects.kapikua_api.administracion;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Clock;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

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
        var reporte = tipo(tipo);
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

    @GetMapping("/{tipo}/excel")
    ResponseEntity<byte[]> excel(@PathVariable String tipo, @RequestParam(required = false) String dia,
                                 @RequestParam(required = false) String mes, @RequestParam(required = false) String estado) {
        var reporte = tipo(tipo);
        if (reporte.isEmpty()) return ResponseEntity.notFound().build();
        var t = reporte.get();
        var periodo = Periodo.de(dia, mes, LocalDate.now(clock));
        var filtro = t.estadoValido(estado);
        var ahora = LocalDateTime.now(clock);
        int limite = ReportesConsulta.MAX_FILAS_DESCARGA;
        byte[] archivo = switch (t) {
            case CLIENTES -> ReporteExcel.generar(t, periodo, filtro, consulta.clientes(periodo, filtro, limite), ReporteExcel.CLIENTES, ahora);
            case TRANSACCIONES -> ReporteExcel.generar(t, periodo, filtro, consulta.transacciones(periodo, filtro, limite), ReporteExcel.TRANSACCIONES, ahora);
            case CREDITOS -> ReporteExcel.generar(t, periodo, filtro, consulta.creditos(periodo, filtro, limite), ReporteExcel.CREDITOS, ahora);
            case COBRANZA -> ReporteExcel.generar(t, periodo, filtro, consulta.cobranza(periodo, filtro, limite), ReporteExcel.COBRANZA, ahora);
        };
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ReporteExcel.TIPO_CONTENIDO))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(ReporteExcel.nombreArchivo(t, periodo), StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(archivo);
    }

    private static Optional<TipoReporte> tipo(String ruta) {
        return Arrays.stream(TipoReporte.values()).filter(t -> t.ruta().equals(ruta)).findFirst();
    }
}
