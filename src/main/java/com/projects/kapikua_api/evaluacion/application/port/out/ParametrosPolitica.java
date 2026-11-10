package com.projects.kapikua_api.evaluacion.application.port.out;

import com.projects.kapikua_api.evaluacion.domain.error.ErrorEvaluacion;
import com.projects.kapikua_api.evaluacion.domain.model.ParametrosCredito;
import com.projects.kapikua_api.shared.Result;

import java.time.LocalDate;

public interface ParametrosPolitica {
    Result<ParametrosCredito, ErrorEvaluacion> vigentes(LocalDate fecha);
}
