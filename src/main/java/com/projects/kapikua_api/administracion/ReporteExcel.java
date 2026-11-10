package com.projects.kapikua_api.administracion;

import com.projects.kapikua_api.administracion.ReportesConsulta.FilaCliente;
import com.projects.kapikua_api.administracion.ReportesConsulta.FilaCredito;
import com.projects.kapikua_api.administracion.ReportesConsulta.FilaCuota;
import com.projects.kapikua_api.administracion.ReportesConsulta.FilaTransaccion;
import com.projects.kapikua_api.administracion.ReportesConsulta.Reporte;
import com.projects.kapikua_api.shared.Dinero;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

final class ReporteExcel {
    static final String TIPO_CONTENIDO = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final String FECHA_HORA = "dd/mm/yyyy hh:mm";
    private static final String FECHA = "dd/mm/yyyy";
    private static final String SOLES = "\"S/\" #,##0.00";
    private static final String ENTERO = "0";
    private static final DateTimeFormatter GENERADO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    record Columna<T>(String titulo, Function<T, Object> valor, String formato, double ancho) {}

    static final List<Columna<FilaCliente>> CLIENTES = List.of(
            new Columna<>("DNI", FilaCliente::dni, null, 12),
            new Columna<>("Nombre", FilaCliente::nombre, null, 28),
            new Columna<>("Negocio", FilaCliente::negocio, null, 24),
            new Columna<>("Celular", FilaCliente::celular, null, 14),
            new Columna<>("Registro", f -> fechaHora(f.registro()), FECHA_HORA, 17),
            new Columna<>("Nivel", FilaCliente::nivel, ENTERO, 8),
            new Columna<>("Saldo", f -> soles(f.saldo()), SOLES, 14),
            new Columna<>("Billetera", FilaCliente::estadoBilletera, null, 12));

    static final List<Columna<FilaTransaccion>> TRANSACCIONES = List.of(
            new Columna<>("N.°", FilaTransaccion::id, ENTERO, 9),
            new Columna<>("Fecha", f -> fechaHora(f.fecha()), FECHA_HORA, 17),
            new Columna<>("Tipo", FilaTransaccion::tipo, null, 18),
            new Columna<>("Monto", f -> soles(f.monto()), SOLES, 14),
            new Columna<>("Origen", FilaTransaccion::origen, null, 20),
            new Columna<>("Destino", FilaTransaccion::destino, null, 20));

    static final List<Columna<FilaCredito>> CREDITOS = List.of(
            new Columna<>("Código", FilaCredito::codigo, null, 18),
            new Columna<>("DNI", FilaCredito::dni, null, 12),
            new Columna<>("Cliente", FilaCredito::nombre, null, 28),
            new Columna<>("Monto", f -> soles(f.monto()), SOLES, 14),
            new Columna<>("TEA (%)", FilaCredito::tea, "0.00", 9),
            new Columna<>("Cuotas", FilaCredito::cuotas, ENTERO, 8),
            new Columna<>("Estado", FilaCredito::estado, null, 11),
            new Columna<>("Desembolso", f -> fechaHora(f.desembolso()), FECHA_HORA, 17),
            new Columna<>("Por cobrar", f -> soles(f.porCobrar()), SOLES, 14));

    static final List<Columna<FilaCuota>> COBRANZA = List.of(
            new Columna<>("Préstamo", FilaCuota::prestamoId, ENTERO, 10),
            new Columna<>("DNI", FilaCuota::dni, null, 12),
            new Columna<>("Cuota", FilaCuota::numero, ENTERO, 7),
            new Columna<>("Vence", FilaCuota::vencimiento, FECHA, 12),
            new Columna<>("Total", f -> soles(f.total()), SOLES, 13),
            new Columna<>("Pagado", f -> soles(f.pagado()), SOLES, 13),
            new Columna<>("Moratorio", f -> soles(f.moratorio()), SOLES, 13),
            new Columna<>("Estado", FilaCuota::estado, null, 11),
            new Columna<>("Fecha de pago", FilaCuota::fechaPago, FECHA, 13),
            new Columna<>("Días de atraso", FilaCuota::diasAtraso, ENTERO, 13));

    private ReporteExcel() {}

    static <T> byte[] generar(TipoReporte tipo, Periodo periodo, String filtro, Reporte<T> reporte,
                              List<Columna<T>> columnas, LocalDateTime generado) {
        var salida = new ByteArrayOutputStream();
        try (var libro = new Workbook(salida, "Kapikua", "1.0")) {
            Worksheet hoja = libro.newWorksheet(tipo.nombre());
            int fila = 0;
            hoja.value(fila, 0, "Kapikua · Reporte de " + tipo.nombre().toLowerCase() + " · " + periodo.etiqueta());
            hoja.style(fila, 0).bold().fontSize(14).set();
            fila++;
            hoja.value(fila++, 0, tipo.filtro() + ": " + (filtro == null ? "Todos" : filtro));
            hoja.value(fila++, 0, "Generado el " + generado.format(GENERADO) + " (hora de Lima). Solo lectura; el DNI va enmascarado.");
            fila++;

            for (var indicador : reporte.indicadores()) {
                hoja.value(fila, 0, indicador.etiqueta());
                hoja.value(fila, 1, indicador.valor());
                hoja.style(fila, 0).bold().set();
                fila++;
            }
            fila++;

            int titulos = fila;
            for (int c = 0; c < columnas.size(); c++) {
                hoja.value(titulos, c, columnas.get(c).titulo());
                hoja.width(c, columnas.get(c).ancho());
            }
            hoja.range(titulos, 0, titulos, columnas.size() - 1).style()
                    .bold().fillColor("DCE6F0").borderStyle("thin").set();
            hoja.freezePane(0, titulos + 1);
            fila++;

            if (reporte.vacio()) {
                hoja.value(fila, 0, "No hay datos para los filtros elegidos.");
            }
            for (T registro : reporte.filas()) {
                for (int c = 0; c < columnas.size(); c++) {
                    var col = columnas.get(c);
                    var valor = col.valor().apply(registro);
                    if (valor == null) continue;
                    escribir(hoja, fila, c, valor);
                    if (col.formato() != null) hoja.style(fila, c).format(col.formato()).set();
                }
                fila++;
            }
            if (reporte.truncado()) {
                hoja.value(fila + 1, 0, "Se incluyen las primeras " + reporte.filas().size()
                        + " filas. Filtra por día o estado para descargar el resto; los indicadores sí cubren todo el período.");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el Excel del reporte", e);
        }
        return salida.toByteArray();
    }

    static String nombreArchivo(TipoReporte tipo, Periodo periodo) {
        return "kapikua-" + tipo.ruta() + "-" + (periodo.porDia() ? periodo.dia() : periodo.mes()) + ".xlsx";
    }

    private static void escribir(Worksheet hoja, int fila, int columna, Object valor) {
        switch (valor) {
            case Number n -> hoja.value(fila, columna, n);
            case LocalDateTime f -> hoja.value(fila, columna, f);
            case LocalDate f -> hoja.value(fila, columna, f);
            case Boolean b -> hoja.value(fila, columna, b);
            default -> hoja.value(fila, columna, valor.toString());
        }
    }

    private static Object soles(Dinero dinero) {
        return dinero == null ? null : dinero.valor();
    }

    private static Object fechaHora(OffsetDateTime fecha) {
        return fecha == null ? null : fecha.toLocalDateTime();
    }
}
