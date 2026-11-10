package com.projects.kapikua_api.identidad.application.port.in;

import com.projects.kapikua_api.identidad.domain.model.TipoConsentimiento;
import com.projects.kapikua_api.shared.UsuarioId;

import java.util.Optional;

/** Para otros contextos: datos del usuario sin exponer la entidad Usuario. */
public interface ConsultarIdentidadUseCase {

    Optional<DatosIdentidad> porId(UsuarioId usuarioId);

    /** RN-07: si el usuario tiene vigente (otorgado y no revocado) ese consentimiento. */
    boolean consentimientoVigente(UsuarioId usuarioId, TipoConsentimiento tipo);

    /**
     * @param dni completo, solo para consultar sistemas externos (Central de Riesgos). Nunca se muestra:
     *            en pantallas y reportes se usa {@code dniEnmascarado} (RN-30).
     */
    record DatosIdentidad(UsuarioId usuarioId, String dni, String nombres, String apellidos, String correo,
                          String dniEnmascarado, boolean identidadValidada, boolean activo) {
        @Override
        public String toString() {
            return "DatosIdentidad[usuarioId=" + usuarioId + ", dni=" + dniEnmascarado + "]";
        }
    }
}
