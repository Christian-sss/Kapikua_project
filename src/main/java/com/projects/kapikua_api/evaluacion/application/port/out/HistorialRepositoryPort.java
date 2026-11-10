package com.projects.kapikua_api.evaluacion.application.port.out;

import com.projects.kapikua_api.evaluacion.domain.model.HistorialCrediticio;
import com.projects.kapikua_api.shared.UsuarioId;

import java.util.Optional;

public interface HistorialRepositoryPort {

    /** Vacío si el cliente nunca tuvo un crédito: se trata como nivel 1 sin historial. */
    Optional<HistorialCrediticio> porUsuario(UsuarioId usuarioId);

    /** Con bloqueo optimista (@Version). */
    HistorialCrediticio guardar(HistorialCrediticio historial);
}
