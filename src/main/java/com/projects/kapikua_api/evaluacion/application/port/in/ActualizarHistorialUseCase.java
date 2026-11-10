package com.projects.kapikua_api.evaluacion.application.port.in;

import com.projects.kapikua_api.shared.UsuarioId;

/** Escalera de niveles (RN-26, RN-32). La mueven los eventos de Créditos, dentro de su misma transacción. */
public interface ActualizarHistorialUseCase {

    void registrarDesembolso(UsuarioId usuarioId);

    void registrarAtraso(UsuarioId usuarioId, int dias);

    void registrarCreditoPagado(UsuarioId usuarioId, boolean pagadoPorAval);
}
