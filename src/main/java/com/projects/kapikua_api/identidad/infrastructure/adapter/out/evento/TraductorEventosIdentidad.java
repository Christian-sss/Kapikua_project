package com.projects.kapikua_api.identidad.infrastructure.adapter.out.evento;

import com.projects.kapikua_api.identidad.domain.event.UsuarioRegistrado;
import com.projects.kapikua_api.shared.puerto.PublicadorEventos;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Traduce los eventos de dominio de Identidad a eventos de integración, en la misma transacción. */
@Component
class TraductorEventosIdentidad {

    private final PublicadorEventos publicador;

    TraductorEventosIdentidad(PublicadorEventos publicador) {
        this.publicador = publicador;
    }

    @EventListener
    void alRegistrarse(UsuarioRegistrado e) {
        publicador.publicar(new com.projects.kapikua_api.shared.evento.UsuarioRegistrado(e.usuarioId(), e.celular(), e.nombreNegocio()));
    }
}
