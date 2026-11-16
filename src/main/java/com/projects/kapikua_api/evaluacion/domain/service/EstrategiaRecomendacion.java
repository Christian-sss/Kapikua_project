package com.projects.kapikua_api.evaluacion.domain.service;

import com.projects.kapikua_api.evaluacion.domain.model.ContextoEvaluacion;
import com.projects.kapikua_api.evaluacion.domain.model.Evaluacion;

public interface EstrategiaRecomendacion {
    boolean aplica(ContextoEvaluacion contexto);

    Evaluacion evaluar(ContextoEvaluacion contexto);
}
