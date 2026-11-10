package com.projects.kapikua_api.credito.application.port.out;

import com.projects.kapikua_api.shared.UsuarioId;

import java.util.Optional;

public interface ClientesCredito {
    void otorgarConsentimientosDeSolicitud(UsuarioId usuarioId);

    void otorgarConsentimientoDeGarantia(UsuarioId avalId);

    Optional<String> dni(UsuarioId usuarioId);

    String nombreEnmascarado(UsuarioId usuarioId);
}
