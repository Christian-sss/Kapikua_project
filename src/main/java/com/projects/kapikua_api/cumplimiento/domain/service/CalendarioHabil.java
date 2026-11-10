package com.projects.kapikua_api.cumplimiento.domain.service;

import com.projects.kapikua_api.cumplimiento.domain.model.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Set;

/**
 * Días hábiles en el Perú: de lunes a viernes, sin feriados nacionales (RN-29).
 * Los feriados fijos son los de ley vigentes desde 2022; Jueves y Viernes Santo se calculan con la Pascua.
 * Los feriados extraordinarios que decrete el Gobierno no se consideran.
 */
public final class CalendarioHabil {

    private static final Set<MonthDay> FERIADOS_FIJOS = Set.of(
            MonthDay.of(1, 1),    // Año Nuevo
            MonthDay.of(5, 1),    // Día del Trabajo
            MonthDay.of(6, 7),    // Batalla de Arica y Día de la Bandera
            MonthDay.of(6, 29),   // San Pedro y San Pablo
            MonthDay.of(7, 23),   // Día de la Fuerza Aérea
            MonthDay.of(7, 28),   // Fiestas Patrias
            MonthDay.of(7, 29),
            MonthDay.of(8, 6),    // Batalla de Junín
            MonthDay.of(8, 30),   // Santa Rosa de Lima
            MonthDay.of(10, 8),   // Combate de Angamos
            MonthDay.of(11, 1),   // Todos los Santos
            MonthDay.of(12, 8),   // Inmaculada Concepción
            MonthDay.of(12, 9),   // Batalla de Ayacucho
            MonthDay.of(12, 25)); // Navidad

    private CalendarioHabil() {}

    public static boolean esHabil(LocalDate fecha) {
        if (fecha.getDayOfWeek() == DayOfWeek.SATURDAY || fecha.getDayOfWeek() == DayOfWeek.SUNDAY) return false;
        if (FERIADOS_FIJOS.contains(MonthDay.from(fecha))) return false;
        var pascua = pascua(fecha.getYear());
        return !fecha.equals(pascua.minusDays(3)) && !fecha.equals(pascua.minusDays(2));
    }

    /** El día hábil número {@code dias} después de {@code desde} (sin contar {@code desde}). */
    public static LocalDate sumarDiasHabiles(LocalDate desde, int dias) {
        var fecha = desde;
        for (int contados = 0; contados < dias; ) {
            fecha = fecha.plusDays(1);
            if (esHabil(fecha)) contados++;
        }
        return fecha;
    }

    /** Días hábiles desde mañana hasta {@code hasta} incluido; 0 si {@code hasta} ya llegó o pasó. */
    public static int diasHabilesHasta(LocalDate hoy, LocalDate hasta) {
        int dias = 0;
        for (var fecha = hoy.plusDays(1); !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
            if (esHabil(fecha)) dias++;
        }
        return dias;
    }

    /** Domingo de Pascua (algoritmo de Meeus/Jones/Butcher para el calendario gregoriano). */
    static LocalDate pascua(int anio) {
        int a = anio % 19, b = anio / 100, c = anio % 100, d = b / 4, e = b % 4;
        int f = (b + 8) / 25, g = (b - f + 1) / 3, h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4, k = c % 4, l = (32 + 2 * e + 2 * i - h - k) % 7, m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31, dia = (h + l - 7 * m + 114) % 31 + 1;
        return LocalDate.of(anio, mes, dia);
    }
}
