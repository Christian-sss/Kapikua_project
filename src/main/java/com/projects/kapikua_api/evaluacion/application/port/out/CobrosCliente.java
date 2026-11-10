package com.projects.kapikua_api.evaluacion.application.port.out;

import com.projects.kapikua_api.evaluacion.domain.model.CobrosDelPeriodo;
import com.projects.kapikua_api.shared.UsuarioId;

import java.time.LocalDate;
import java.util.Optional;

public interface CobrosCliente {
    Optional<CobrosDelPeriodo> delPeriodo(UsuarioId usuarioId, LocalDate fechaSolicitud, int semanas);
}
