package com.projects.kapikua_api.billetera.application.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.YearMonth;

record PeriodoMensual(OffsetDateTime desde, OffsetDateTime hasta) {
    static PeriodoMensual actual(Clock clock) {
        var mes = YearMonth.now(clock);
        var zona = clock.getZone();
        return new PeriodoMensual(
                mes.atDay(1).atStartOfDay(zona).toOffsetDateTime(),
                mes.plusMonths(1).atDay(1).atStartOfDay(zona).toOffsetDateTime());
    }
}
