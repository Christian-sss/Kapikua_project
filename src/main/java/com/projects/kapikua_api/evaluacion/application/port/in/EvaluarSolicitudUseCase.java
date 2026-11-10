package com.projects.kapikua_api.evaluacion.application.port.in;

import com.projects.kapikua_api.evaluacion.domain.error.ErrorEvaluacion;
import com.projects.kapikua_api.evaluacion.domain.model.SolicitudCredito;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

/**
 * RF-EV-01 (CU-08 y CU-09). Lo llama Créditos. Créditos recibe ResultadoEvaluacion, nunca la entidad Evaluacion.
 */
public interface EvaluarSolicitudUseCase {

    /**
     * Ok con la recomendación (APTO, APTO_CON_AJUSTE o NO_APTO), ya guardada de forma inmutable.
     * Err si no se pudo evaluar (falta consentimiento, uso mínimo, cuotas fuera de rango…): no se guarda nada.
     */
    Result<ResultadoEvaluacion, ErrorEvaluacion> evaluar(UsuarioId usuarioId, SolicitudCredito solicitud);

    /** RN-21: identidad validada, clasificación Normal y sin bloqueo. Créditos revisa el resto (garantías, mora). */
    Result<Void, ErrorEvaluacion> evaluarAval(UsuarioId avalId);
}
