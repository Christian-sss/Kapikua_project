package com.projects.kapikua_api.identidad.dto;

import com.projects.kapikua_api.shared.UsuarioId;

public record DatosIdentidad(
        UsuarioId usuarioId,
        String dni,
        String nombres,
        String apellidos,
        String correo,
        String dniEnmascarado,
        boolean identidadValidada,
        boolean activo
) {
    @Override
    public String toString() {
        return "DatosIdentidad[usuarioId=" + usuarioId + ", dni=" + dniEnmascarado + "]";
    }
}
