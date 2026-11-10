package com.projects.kapikua_api.credito.application.port.out;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.credito.domain.model.CondicionesCredito;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;

import java.math.BigDecimal;

public interface EntidadFinanciera {
    record SolicitudEfa(String dniCliente, Dinero monto, int cuotas, BigDecimal tea, Dinero cuota, Integer score,
                        String recomendacion, String dniAval) {
        @Override
        public String toString() { return "SolicitudEfa[monto=" + monto + ", cuotas=" + cuotas + "]"; }
    }

    sealed interface RespuestaEfa {
        record Confirmada(String codigoSolicitud, CondicionesCredito condiciones) implements RespuestaEfa {}
        record Denegada(String codigoSolicitud, String motivo) implements RespuestaEfa {}
    }

    Result<RespuestaEfa, ErrorCredito> solicitarDecision(SolicitudEfa solicitud);

    Result<String, ErrorCredito> confirmarDesembolso(String codigoSolicitud);

    Result<Void, ErrorCredito> registrarPago(String codigoCredito, Dinero monto);
}
