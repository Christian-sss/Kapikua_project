package com.projects.kapikua_api.shared.infrastructure;

import com.projects.kapikua_api.shared.puerto.PublicadorEventos;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
class SpringPublicadorEventos implements PublicadorEventos {

    private final ApplicationEventPublisher publisher;

    SpringPublicadorEventos(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publicar(Object evento) {
        publisher.publishEvent(evento);
    }
}
