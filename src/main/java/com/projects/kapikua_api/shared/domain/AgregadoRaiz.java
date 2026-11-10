package com.projects.kapikua_api.shared.domain;

import java.util.ArrayList;
import java.util.List;

public abstract class AgregadoRaiz {
    private transient List<EventoDominio> eventos;

    protected void registrar(EventoDominio evento) {
        if (eventos == null) eventos = new ArrayList<>();
        eventos.add(evento);
    }

    public List<EventoDominio> extraerEventos() {
        if (eventos == null || eventos.isEmpty()) return List.of();
        var pendientes = List.copyOf(eventos);
        eventos.clear();
        return pendientes;
    }
}
