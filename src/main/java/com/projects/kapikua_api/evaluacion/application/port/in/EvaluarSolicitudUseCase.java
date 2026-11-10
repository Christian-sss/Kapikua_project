package com.projects.kapikua_api.evaluacion.application.port.in;

import com.projects.kapikua_api.evaluacion.domain.error.ErrorEvaluacion;
import com.projects.kapikua_api.evaluacion.domain.model.SolicitudCredito;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

public interface EvaluarSolicitudUseCase {
    Result<ResultadoEvaluacion, ErrorEvaluacion> evaluar(UsuarioId usuarioId, SolicitudCredito solicitud);

    Result<Void, ErrorEvaluacion> evaluarAval(UsuarioId avalId);
}
