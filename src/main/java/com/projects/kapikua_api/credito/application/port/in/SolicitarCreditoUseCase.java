package com.projects.kapikua_api.credito.application.port.in;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.evaluacion.domain.model.Destino;
import com.projects.kapikua_api.evaluacion.domain.model.Recomendacion;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

public interface SolicitarCreditoUseCase {
    Result<ResultadoSolicitud, ErrorCredito> solicitar(UsuarioId usuarioId, Dinero monto, int cuotas, Destino destino,
                                                       boolean autorizaDatos);

    record ResultadoSolicitud(Long ofertaId, Recomendacion recomendacion, String motivo, boolean requiereAval) {}
}
