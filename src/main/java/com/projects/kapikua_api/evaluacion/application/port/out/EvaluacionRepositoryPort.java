package com.projects.kapikua_api.evaluacion.application.port.out;

import com.projects.kapikua_api.evaluacion.domain.model.Evaluacion;

import java.util.Optional;

public interface EvaluacionRepositoryPort {
    Evaluacion registrar(Evaluacion evaluacion);

    Optional<Evaluacion> porId(Long id);
}
