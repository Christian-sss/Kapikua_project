package com.projects.kapikua_api.identidad.application.port.in;

import com.projects.kapikua_api.identidad.domain.model.TipoConsentimiento;
import com.projects.kapikua_api.shared.UsuarioId;

/**
 * RN-07: consentimientos separados, versionados y revocables (Ley 29733). Créditos los pide al solicitar
 * (EVALUACION_CON_BILLETERA, ENVIO_DATOS_EFA) y el aval al aceptar (GARANTIA_AVAL).
 */
public interface GestionarConsentimientosUseCase {

    /** Si ya está vigente no hace nada. Devuelve false si el usuario no existe. */
    boolean otorgar(UsuarioId usuarioId, TipoConsentimiento tipo);

    /** Si no está vigente no hace nada. Devuelve false si el usuario no existe. */
    boolean revocar(UsuarioId usuarioId, TipoConsentimiento tipo);
}
