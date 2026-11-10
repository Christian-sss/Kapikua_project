package com.projects.kapikua_api.evaluacion.application.port.in;

import com.projects.kapikua_api.evaluacion.domain.model.CondicionesSugeridas;
import com.projects.kapikua_api.evaluacion.domain.model.Evaluacion;
import com.projects.kapikua_api.evaluacion.domain.model.Recomendacion;
import com.projects.kapikua_api.shared.UsuarioId;

import java.time.OffsetDateTime;

/** Lo que Créditos y la pantalla ven de una evaluación. {@code condiciones} es null en un NO_APTO. */
public record ResultadoEvaluacion(Long evaluacionId, UsuarioId usuarioId, Recomendacion recomendacion, Integer score,
                                  CondicionesSugeridas condiciones, boolean requiereAval, String motivo,
                                  OffsetDateTime fecha) {

    public static ResultadoEvaluacion de(Evaluacion e) {
        return new ResultadoEvaluacion(e.id(), e.usuarioId(), e.recomendacion(), e.score(), e.condiciones(),
                e.requiereAval(), e.motivo(), e.fecha());
    }
}
