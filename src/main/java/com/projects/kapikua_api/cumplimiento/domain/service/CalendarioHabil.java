package com.projects.kapikua_api.cumplimiento.domain.service;

import com.projects.kapikua_api.cumplimiento.domain.model.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Set;

public final class CalendarioHabil {
    private static final Set<MonthDay> FERIADOS_FIJOS = Set.of(
            MonthDay.of(1, 1),
            MonthDay.of(5, 1),
            MonthDay.of(6, 7),
            MonthDay.of(6, 29),
            MonthDay.of(7, 23),
            MonthDay.of(7, 28),
            MonthDay.of(7, 29),
            MonthDay.of(8, 6),
            MonthDay.of(8, 30),
            MonthDay.of(10, 8),
            MonthDay.of(11, 1),
            MonthDay.of(12, 8),
            MonthDay.of(12, 9),
            MonthDay.of(12, 25));

    private CalendarioHabil() {}

    public static boolean esHabil(LocalDate fecha) {
        if (fecha.getDayOfWeek() == DayOfWeek.SATURDAY || fecha.getDayOfWeek() == DayOfWeek.SUNDAY) return false;
        if (FERIADOS_FIJOS.contains(MonthDay.from(fecha))) return false;
        var pascua = pascua(fecha.getYear());
        return !fecha.equals(pascua.minusDays(3)) && !fecha.equals(pascua.minusDays(2));
    }

    public static LocalDate sumarDiasHabiles(LocalDate desde, int dias) {
        var fecha = desde;
        for (int contados = 0; contados < dias; ) {
            fecha = fecha.plusDays(1);
            if (esHabil(fecha)) contados++;
        }
        return fecha;
    }

    public static int diasHabilesHasta(LocalDate hoy, LocalDate hasta) {
        int dias = 0;
        for (var fecha = hoy.plusDays(1); !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
            if (esHabil(fecha)) dias++;
        }
        return dias;
    }

    static LocalDate pascua(int anio) {
        int a = anio % 19, b = anio / 100, c = anio % 100, d = b / 4, e = b % 4;
        int f = (b + 8) / 25, g = (b - f + 1) / 3, h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4, k = c % 4, l = (32 + 2 * e + 2 * i - h - k) % 7, m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31, dia = (h + l - 7 * m + 114) % 31 + 1;
        return LocalDate.of(anio, mes, dia);
    }
}
