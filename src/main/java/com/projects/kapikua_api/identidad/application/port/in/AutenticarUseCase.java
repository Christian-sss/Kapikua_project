package com.projects.kapikua_api.identidad.application.port.in;

import com.projects.kapikua_api.identidad.domain.error.AutenticacionError;
import com.projects.kapikua_api.identidad.domain.model.Rol;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

public interface AutenticarUseCase {
    Result<SesionEmitida, AutenticacionError> autenticar(String correoODni, String contrasena);

    record SesionEmitida(UsuarioId usuarioId, Rol rol, String token) {
        @Override
        public String toString() {
            return "SesionEmitida[usuarioId=" + usuarioId + ", rol=" + rol + ", token=****]";
        }
    }
}
