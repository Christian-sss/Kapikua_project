package com.projects.kapikua_api.credito.application.port.out;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.evaluacion.application.port.in.ConsultarEvaluacionUseCase.NivelCliente;
import com.projects.kapikua_api.evaluacion.application.port.in.ResultadoEvaluacion;
import com.projects.kapikua_api.evaluacion.domain.model.Destino;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

import java.util.Optional;

/** Evaluación Crediticia: Créditos recibe su ResultadoEvaluacion, nunca la entidad Evaluacion. */
public interface EvaluacionCrediticia {

    Result<ResultadoEvaluacion, ErrorCredito> evaluar(UsuarioId usuarioId, Dinero monto, int cuotas, Destino destino);

    /** RN-21: identidad validada, clasificación Normal y sin bloqueo. */
    Result<Void, ErrorCredito> evaluarAval(UsuarioId avalId);

    Optional<ResultadoEvaluacion> evaluacion(Long evaluacionId);

    Result<NivelCliente, ErrorCredito> nivel(UsuarioId usuarioId);
}
