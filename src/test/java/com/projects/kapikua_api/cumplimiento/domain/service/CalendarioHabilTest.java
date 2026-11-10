package com.projects.kapikua_api.cumplimiento.domain.service;

import com.projects.kapikua_api.cumplimiento.domain.model.*;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarioHabilTest {

    @Test
    void quinceDiasHabilesSaltanFinesDeSemana() {
        assertThat(CalendarioHabil.sumarDiasHabiles(LocalDate.of(2026, 11, 9), 15)).isEqualTo(LocalDate.of(2026, 11, 30));
    }

    @Test
    void saltanLosFeriadosNacionales() {
        // 8 y 9 de diciembre
        assertThat(CalendarioHabil.sumarDiasHabiles(LocalDate.of(2026, 12, 1), 15)).isEqualTo(LocalDate.of(2026, 12, 24));
        // 23, 28 y 29 de julio
        assertThat(CalendarioHabil.sumarDiasHabiles(LocalDate.of(2026, 7, 20), 5)).isEqualTo(LocalDate.of(2026, 7, 30));
    }

    @Test
    void jueves_y_viernes_santo_dependen_de_la_pascua() {
        assertThat(CalendarioHabil.pascua(2025)).isEqualTo(LocalDate.of(2025, 4, 20));
        assertThat(CalendarioHabil.pascua(2026)).isEqualTo(LocalDate.of(2026, 4, 5));
        assertThat(CalendarioHabil.pascua(2027)).isEqualTo(LocalDate.of(2027, 3, 28));
        assertThat(CalendarioHabil.esHabil(LocalDate.of(2026, 4, 2))).isFalse();
        assertThat(CalendarioHabil.esHabil(LocalDate.of(2026, 4, 3))).isFalse();
        assertThat(CalendarioHabil.sumarDiasHabiles(LocalDate.of(2026, 3, 31), 2)).isEqualTo(LocalDate.of(2026, 4, 6));
    }

    @Test
    void diasHabilesHastaLaFechaLimite() {
        var lunes = LocalDate.of(2026, 11, 9);
        assertThat(CalendarioHabil.diasHabilesHasta(lunes, LocalDate.of(2026, 11, 13))).isEqualTo(4);
        assertThat(CalendarioHabil.diasHabilesHasta(lunes, lunes)).isZero();
        assertThat(CalendarioHabil.diasHabilesHasta(lunes, lunes.minusDays(3))).isZero();
    }
}
