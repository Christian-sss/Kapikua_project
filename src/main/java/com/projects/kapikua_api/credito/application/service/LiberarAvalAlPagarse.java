package com.projects.kapikua_api.credito.application.service;

import com.projects.kapikua_api.credito.application.port.out.OfertaRepositoryPort;
import com.projects.kapikua_api.credito.domain.event.PrestamoPagado;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;

@Component
class LiberarAvalAlPagarse {
    private final OfertaRepositoryPort ofertas;
    private final Clock clock;

    LiberarAvalAlPagarse(OfertaRepositoryPort ofertas, Clock clock) {
        this.ofertas = ofertas;
        this.clock = clock;
    }

    @EventListener
    void alPagarse(PrestamoPagado e) {
        var oferta = ofertas.porId(e.ofertaId()).orElseThrow();
        oferta.liberarAval(OffsetDateTime.now(clock));
        ofertas.guardar(oferta);
    }
}
