package com.projects.kapikua_api.identidad.application.port.in;

import com.projects.kapikua_api.identidad.domain.model.TipoConsentimiento;
import com.projects.kapikua_api.shared.UsuarioId;

import java.util.Optional;

public interface ConsultarIdentidadUseCase {
    Optional<DatosIdentidad> porId(UsuarioId usuarioId);

    boolean consentimientoVigente(UsuarioId usuarioId, TipoConsentimiento tipo);

    record DatosIdentidad(UsuarioId usuarioId, String dni, String nombres, String apellidos, String correo,
                          String dniEnmascarado, boolean identidadValidada, boolean activo) {
        @Override
        public String toString() {
            return "DatosIdentidad[usuarioId=" + usuarioId + ", dni=" + dniEnmascarado + "]";
        }
    }
}
