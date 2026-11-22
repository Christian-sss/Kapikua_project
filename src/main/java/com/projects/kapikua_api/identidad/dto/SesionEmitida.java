package com.projects.kapikua_api.identidad.dto;

import com.projects.kapikua_api.identidad.entity.Rol;
import com.projects.kapikua_api.shared.UsuarioId;

public record SesionEmitida(UsuarioId usuarioId, Rol rol, String token) {
    @Override
    public String toString() {
        return "SesionEmitida[usuarioId=" + usuarioId + ", rol=" + rol + ", token=****]";
    }
}
