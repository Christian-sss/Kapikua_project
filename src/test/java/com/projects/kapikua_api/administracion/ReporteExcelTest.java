package com.projects.kapikua_api.administracion;

import com.projects.kapikua_api.administracion.ReportesConsulta.FilaTransaccion;
import com.projects.kapikua_api.administracion.ReportesConsulta.Indicador;
import com.projects.kapikua_api.administracion.ReportesConsulta.Reporte;
import com.projects.kapikua_api.shared.Dinero;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;

/** CU-16: el Excel trae las columnas de la pantalla, montos y fechas como valores de Excel y el DNI enmascarado. */
class ReporteExcelTest {

    static final LocalDateTime GENERADO = LocalDateTime.of(2026, 11, 10, 15, 30);
    static final Periodo NOVIEMBRE = Periodo.de(null, "2026-11", LocalDate.of(2026, 11, 10));

    /**
     * Un .xlsx es un zip de XML: devuelve cada parte por su nombre. Se lee con ZipFile (el directorio central),
     * como Excel: fastexcel escribe en streaming y ZipInputStream no lee los tamaños que van al final de cada parte.
     */
    static Map<String, String> partes(byte[] xlsx) throws IOException {
        var archivo = Files.createTempFile("reporte", ".xlsx");
        try {
            Files.write(archivo, xlsx);
            var partes = new HashMap<String, String>();
            try (var zip = new ZipFile(archivo.toFile())) {
                for (var e : java.util.Collections.list(zip.entries())) {
                    try (var in = zip.getInputStream(e)) {
                        partes.put(e.getName(), sinEntidades(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
                    }
                }
            }
            return partes;
        } finally {
            Files.deleteIfExists(archivo);
        }
    }

    /** El XML escribe los caracteres no ASCII como entidades (&#xb7; = ·); Excel los muestra normalmente. */
    static String sinEntidades(String xml) {
        return java.util.regex.Pattern.compile("&#x([0-9a-fA-F]+);").matcher(xml)
                .replaceAll(m -> java.util.regex.Matcher.quoteReplacement(Character.toString(Integer.parseInt(m.group(1), 16))));
    }

    static Reporte<FilaTransaccion> reporte(boolean truncado, FilaTransaccion... filas) {
        return new Reporte<>(List.of(filas), truncado, List.of(new Indicador("Operaciones", String.valueOf(filas.length))), List.of());
    }

    static final FilaTransaccion PAGO = new FilaTransaccion(137L,
            OffsetDateTime.of(2026, 11, 7, 18, 0, 0, 0, ZoneOffset.ofHours(-5)), "PAGO_NEGOCIO",
            Dinero.de("1300.50"), "DNI ****8914", "DNI ****0002");

    @Test
    void traeTituloFiltrosIndicadoresYColumnasDeLaPantalla() throws IOException {
        var xlsx = ReporteExcel.generar(TipoReporte.TRANSACCIONES, NOVIEMBRE, "PAGO_NEGOCIO", reporte(false, PAGO),
                ReporteExcel.TRANSACCIONES, GENERADO);
        var textos = partes(xlsx).get("xl/sharedStrings.xml");

        assertThat(textos).contains("Kapikua · Reporte de transacciones · noviembre de 2026")
                .contains("Tipo: PAGO_NEGOCIO")
                .contains("Generado el 10/11/2026 15:30 (hora de Lima)")
                .contains("Operaciones")
                .contains("N.°", "Fecha", "Monto", "Origen", "Destino")
                .contains("DNI ****8914", "DNI ****0002");
    }

    @Test
    void montosYFechasSonValoresDeExcelNoTexto() throws IOException {
        var p = partes(ReporteExcel.generar(TipoReporte.TRANSACCIONES, NOVIEMBRE, null, reporte(false, PAGO),
                ReporteExcel.TRANSACCIONES, GENERADO));
        var hoja = p.values().stream().filter(x -> x.contains("<sheetData")).findFirst().orElseThrow();

        assertThat(hoja).contains("t=\"n\"><v>1300.50</v>");       // el monto es un número que se puede sumar
        assertThat(hoja).contains("t=\"n\"><v>46333.75</v>");      // 07/11/2026 18:00 como fecha serial de Excel (hora de Lima)
        assertThat(p.get("xl/styles.xml")).contains("dd/mm/yyyy hh:mm").contains("&quot;S/&quot; #,##0.00");
        assertThat(p.get("xl/sharedStrings.xml")).contains("Tipo: Todos");
    }

    @Test
    void avisaCuandoNoHayDatosOCuandoSeCortaronFilas() throws IOException {
        assertThat(partes(ReporteExcel.generar(TipoReporte.TRANSACCIONES, NOVIEMBRE, null, reporte(false),
                ReporteExcel.TRANSACCIONES, GENERADO)).get("xl/sharedStrings.xml"))
                .contains("No hay datos para los filtros elegidos.");
        assertThat(partes(ReporteExcel.generar(TipoReporte.TRANSACCIONES, NOVIEMBRE, null, reporte(true, PAGO),
                ReporteExcel.TRANSACCIONES, GENERADO)).get("xl/sharedStrings.xml"))
                .contains("Se incluyen las primeras 1 filas");
    }

    @Test
    void elNombreDelArchivoLlevaElReporteYElPeriodo() {
        assertThat(ReporteExcel.nombreArchivo(TipoReporte.COBRANZA, NOVIEMBRE)).isEqualTo("kapikua-cobranza-2026-11.xlsx");
        assertThat(ReporteExcel.nombreArchivo(TipoReporte.CLIENTES, Periodo.de("2026-11-07", null, LocalDate.of(2026, 11, 10))))
                .isEqualTo("kapikua-clientes-2026-11-07.xlsx");
    }

    @Test
    void cadaReporteTieneSusColumnas() {
        assertThat(ReporteExcel.CLIENTES).extracting(ReporteExcel.Columna::titulo).startsWith("DNI", "Nombre");
        assertThat(ReporteExcel.CREDITOS).extracting(ReporteExcel.Columna::titulo).contains("Código", "TEA (%)", "Por cobrar");
        assertThat(ReporteExcel.COBRANZA).extracting(ReporteExcel.Columna::titulo).contains("Vence", "Días de atraso");
    }
}
