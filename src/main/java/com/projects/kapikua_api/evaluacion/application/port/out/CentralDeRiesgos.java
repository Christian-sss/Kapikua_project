package com.projects.kapikua_api.evaluacion.application.port.out;

import com.projects.kapikua_api.evaluacion.domain.error.ErrorEvaluacion;
import com.projects.kapikua_api.evaluacion.domain.model.InformeCentral;
import com.projects.kapikua_api.shared.Result;

public interface CentralDeRiesgos {
    Result<InformeCentral, ErrorEvaluacion> consultar(String dni);
}
