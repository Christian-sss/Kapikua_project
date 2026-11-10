package com.projects.kapikua_api.evaluacion.application.port.in;

import com.projects.kapikua_api.shared.UsuarioId;

public interface ActualizarHistorialUseCase {
    void registrarDesembolso(UsuarioId usuarioId);

    void registrarAtraso(UsuarioId usuarioId, int dias);

    void registrarCreditoPagado(UsuarioId usuarioId, boolean pagadoPorAval);
}
