package com.projects.kapikua_api.shared;

public record UsuarioId(Long valor) {
    public UsuarioId {
        if (valor == null || valor <= 0) {
            throw new IllegalArgumentException("UsuarioId inválido: " + valor);
        }
    }
}
