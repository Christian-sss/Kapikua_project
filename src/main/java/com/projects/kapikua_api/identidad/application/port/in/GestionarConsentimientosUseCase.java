package com.projects.kapikua_api.identidad.application.port.in;

import com.projects.kapikua_api.identidad.domain.model.TipoConsentimiento;
import com.projects.kapikua_api.shared.UsuarioId;

public interface GestionarConsentimientosUseCase {
    boolean otorgar(UsuarioId usuarioId, TipoConsentimiento tipo);

    boolean revocar(UsuarioId usuarioId, TipoConsentimiento tipo);
}
