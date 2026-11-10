package com.projects.kapikua_api.shared.puerto;

import com.projects.kapikua_api.shared.domain.AgregadoRaiz;
import com.projects.kapikua_api.shared.domain.EventoDominio;

import java.util.Collection;

/**
 * Publica eventos dentro de la transacción en curso. La aplicación publica los eventos de dominio que
 * registraron los agregados; los traductores de cada contexto los convierten en eventos de integración.
 */
public interface PublicadorEventos {

    void publicar(Object evento);

    /** Publica (y borra) los eventos pendientes del agregado. Se llama después de guardarlo. */
    default void publicarDe(AgregadoRaiz agregado) {
        agregado.extraerEventos().forEach(this::publicar);
    }

    default void publicarTodos(Collection<? extends EventoDominio> eventos) {
        eventos.forEach(this::publicar);
    }
}
