package com.projects.kapikua_api.shared.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Raíz de un agregado que registra eventos de dominio. Los eventos quedan pendientes hasta que la aplicación,
 * después de guardar el agregado, los extrae y los publica ({@code PublicadorEventos.publicarDe}).
 * La lista es {@code transient}: no se persiste (tampoco cuando la subclase es una entidad JPA).
 */
public abstract class AgregadoRaiz {

    private transient List<EventoDominio> eventos;

    protected void registrar(EventoDominio evento) {
        if (eventos == null) eventos = new ArrayList<>();
        eventos.add(evento);
    }

    /** Devuelve los eventos pendientes y los borra: cada evento se publica una sola vez. */
    public List<EventoDominio> extraerEventos() {
        if (eventos == null || eventos.isEmpty()) return List.of();
        var pendientes = List.copyOf(eventos);
        eventos.clear();
        return pendientes;
    }
}
