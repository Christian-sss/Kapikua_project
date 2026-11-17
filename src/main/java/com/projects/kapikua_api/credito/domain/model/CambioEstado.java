package com.projects.kapikua_api.credito.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;

public record CambioEstado(String anterior, String nuevo, String motivo, OffsetDateTime fecha) {
    public CambioEstado {
        Objects.requireNonNull(anterior);
        Objects.requireNonNull(nuevo);
        Objects.requireNonNull(motivo);
        Objects.requireNonNull(fecha);
    }
}
