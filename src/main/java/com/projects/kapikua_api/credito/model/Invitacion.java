package com.projects.kapikua_api.credito.model;

import com.projects.kapikua_api.shared.UsuarioId;

import java.time.OffsetDateTime;
import java.util.Objects;

public record Invitacion(
        UsuarioId usuarioDeudorId,
        UsuarioId usuarioAvalId,
        OffsetDateTime fechaInvitacion
) {

    public Invitacion {
        Objects.requireNonNull(usuarioDeudorId);
        Objects.requireNonNull(usuarioAvalId);
        Objects.requireNonNull(fechaInvitacion);


        if (usuarioDeudorId.equals(usuarioAvalId)) {
            throw new IllegalArgumentException("El aval no puede ser el deudor");
        }
    }

}
