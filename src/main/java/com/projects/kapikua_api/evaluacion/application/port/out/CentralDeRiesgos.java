package com.projects.kapikua_api.evaluacion.application.port.out;

import com.projects.kapikua_api.evaluacion.domain.error.ErrorEvaluacion;
import com.projects.kapikua_api.evaluacion.domain.model.InformeCentral;
import com.projects.kapikua_api.shared.Result;

/** Central de Riesgos de la SBS: clasificación del deudor y deuda total en el sistema (N8, N9). Simulada en la v1. */
public interface CentralDeRiesgos {

    Result<InformeCentral, ErrorEvaluacion> consultar(String dni);
}
