package com.projects.kapikua_api.evaluacion.application.port.out;

import com.projects.kapikua_api.evaluacion.domain.model.Evaluacion;

import java.util.Optional;

/** Solo inserción: una evaluación emitida no se modifica (RN-19; un trigger de la base lo impide). */
public interface EvaluacionRepositoryPort {

    Evaluacion registrar(Evaluacion evaluacion);

    Optional<Evaluacion> porId(Long id);
}
