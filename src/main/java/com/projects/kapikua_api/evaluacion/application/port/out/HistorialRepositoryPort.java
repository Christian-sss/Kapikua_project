package com.projects.kapikua_api.evaluacion.application.port.out;

import com.projects.kapikua_api.evaluacion.domain.model.HistorialCrediticio;
import com.projects.kapikua_api.shared.UsuarioId;

import java.util.Optional;

public interface HistorialRepositoryPort {
    Optional<HistorialCrediticio> porUsuario(UsuarioId usuarioId);

    HistorialCrediticio guardar(HistorialCrediticio historial);
}
