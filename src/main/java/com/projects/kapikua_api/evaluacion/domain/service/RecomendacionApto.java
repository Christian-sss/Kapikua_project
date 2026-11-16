package com.projects.kapikua_api.evaluacion.domain.service;

import com.projects.kapikua_api.evaluacion.domain.model.ContextoEvaluacion;
import com.projects.kapikua_api.evaluacion.domain.model.Evaluacion;

public final class RecomendacionApto implements EstrategiaRecomendacion {
    @Override
    public boolean aplica(ContextoEvaluacion contexto) {
        return contexto.ofertaViable().isPresent() && !contexto.requiereAjustePorRiesgo()
                && !contexto.oferta().condiciones().monto().menorQue(contexto.solicitud().monto());
    }

    @Override
    public Evaluacion evaluar(ContextoEvaluacion contexto) {
        return Evaluacion.apto(contexto, contexto.oferta().condiciones());
    }
}
