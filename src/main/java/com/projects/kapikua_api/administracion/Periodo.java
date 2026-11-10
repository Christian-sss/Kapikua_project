package com.projects.kapikua_api.administracion;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * CU-16, 2: filtro por día o por mes, en hora de Lima. {@code hasta} no se incluye.
 * Sin filtro (o con uno inválido) se usa el mes en curso; {@code aviso} explica el cambio.
 */
public record Periodo(LocalDate desde, LocalDate hasta, boolean porDia, String etiqueta, String aviso) {

    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale.forLanguageTag("es-PE"));
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static Periodo de(String dia, String mes, LocalDate hoy) {
        try {
            if (dia != null && !dia.isBlank()) {
                var d = LocalDate.parse(dia);
                return new Periodo(d, d.plusDays(1), true, d.format(DIA), null);
            }
            if (mes != null && !mes.isBlank()) return delMes(YearMonth.parse(mes), null);
        } catch (DateTimeParseException e) {
            return delMes(YearMonth.from(hoy), "La fecha no es válida; se muestra el mes en curso.");
        }
        return delMes(YearMonth.from(hoy), null);
    }

    private static Periodo delMes(YearMonth m, String aviso) {
        return new Periodo(m.atDay(1), m.plusMonths(1).atDay(1), false, m.format(MES), aviso);
    }

    public OffsetDateTime inicio(ZoneId zona) {
        return desde.atStartOfDay(zona).toOffsetDateTime();
    }

    public OffsetDateTime fin(ZoneId zona) {
        return hasta.atStartOfDay(zona).toOffsetDateTime();
    }

    /** Para volver a llenar el formulario. */
    public String dia() {
        return porDia ? desde.toString() : "";
    }

    public String mes() {
        return YearMonth.from(desde).toString();
    }
}
