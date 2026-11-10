package com.projects.kapikua_api.credito.infrastructure.adapter.in.programado;

import com.projects.kapikua_api.credito.application.port.in.CobranzaUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/** CU-13: el temporizador del sistema inicia el cobro diario (por defecto a las 6:00, hora de Lima). */
@Component
class CobranzaProgramada {

    private final CobranzaUseCase cobranza;
    private final Clock clock;

    CobranzaProgramada(CobranzaUseCase cobranza, Clock clock) {
        this.cobranza = cobranza;
        this.clock = clock;
    }

    @Scheduled(cron = "${kapikua.cobranza.cron:0 0 6 * * *}", zone = "America/Lima")
    void cobrarDelDia() {
        cobranza.ejecutarCobroDiario(LocalDate.now(clock));
    }
}
