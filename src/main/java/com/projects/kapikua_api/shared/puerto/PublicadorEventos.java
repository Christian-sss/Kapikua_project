package com.projects.kapikua_api.shared.puerto;

import com.projects.kapikua_api.shared.domain.EventoDominio;

import java.util.Collection;

public interface PublicadorEventos {
    void publicar(Object evento);

    default void publicarTodos(Collection<? extends EventoDominio> eventos) {
        eventos.forEach(this::publicar);
    }
}
