package com.projects.kapikua_api.shared;

/**
 * Identificador del usuario que comparten todos los contextos.
 * Es lo único del usuario que sale de Identidad: los demás datos se piden por un puerto de consulta.
 */
public record UsuarioId(Long valor) {

    public UsuarioId {
        if (valor == null || valor <= 0) {
            throw new IllegalArgumentException("UsuarioId inválido: " + valor);
        }
    }
}
