package com.projects.kapikua_api.credito.application.port.in;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.evaluacion.domain.model.Destino;
import com.projects.kapikua_api.evaluacion.domain.model.Recomendacion;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

/** CU-08 Solicitar crédito (RF-CR-02, RF-EV-01, RF-CR-03). */
public interface SolicitarCreditoUseCase {

    /**
     * @param autorizaDatos el cliente autoriza evaluar con su billetera y enviar los datos a la entidad aliada (RN-07)
     * @return con NO_APTO no se crea oferta ({@code ofertaId} null) y {@code motivo} explica por qué
     */
    Result<ResultadoSolicitud, ErrorCredito> solicitar(UsuarioId usuarioId, Dinero monto, int cuotas, Destino destino,
                                                       boolean autorizaDatos);

    record ResultadoSolicitud(Long ofertaId, Recomendacion recomendacion, String motivo, boolean requiereAval) {}
}
