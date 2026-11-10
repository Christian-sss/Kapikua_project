package com.projects.kapikua_api.identidad.application.port.in;

import com.projects.kapikua_api.identidad.domain.error.AutenticacionError;
import com.projects.kapikua_api.identidad.domain.model.Rol;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

/** CU-02 Iniciar sesión (RF-ID-02): con correo o DNI y contraseña. */
public interface AutenticarUseCase {

    Result<SesionEmitida, AutenticacionError> autenticar(String correoODni, String contrasena);

    /** El token va en una cookie HttpOnly; nunca se muestra en la página. */
    record SesionEmitida(UsuarioId usuarioId, Rol rol, String token) {
        @Override
        public String toString() {
            return "SesionEmitida[usuarioId=" + usuarioId + ", rol=" + rol + ", token=****]";
        }
    }
}
