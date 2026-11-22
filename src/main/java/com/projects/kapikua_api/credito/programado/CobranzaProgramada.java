package com.projects.kapikua_api.credito.programado;

import com.projects.kapikua_api.credito.service.CobranzaService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
class CobranzaProgramada {
    private final CobranzaService cobranza;
    private final Clock clock;

    CobranzaProgramada(CobranzaService cobranza, Clock clock) {
        this.cobranza = cobranza;
        this.clock = clock;
    }

    @Scheduled(cron = "${kapikua.cobranza.cron:0 0 6 * * *}", zone = "America/Lima")
    void cobrarDelDia() {
        cobranza.ejecutarCobroDiario(LocalDate.now(clock));
    }
}
