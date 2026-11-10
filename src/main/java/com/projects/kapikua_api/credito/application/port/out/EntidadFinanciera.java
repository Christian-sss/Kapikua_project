package com.projects.kapikua_api.credito.application.port.out;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.credito.domain.model.CondicionesCredito;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;

import java.math.BigDecimal;

/**
 * Capa anticorrupción hacia Financiera Ayni Sur (RES-01, RN-18): la entidad aliada decide, desembolsa con
 * fondos propios y asume el riesgo. Kapikua solo recomienda. Simulada en la v1. Cambiar de aliado solo exige
 * un nuevo adaptador de este puerto (R04).
 */
public interface EntidadFinanciera {

    /** Lo que se envía: la solicitud, el score y los datos del aval (RF-CR-03). */
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

    /** CU-10, paso 6: la entidad aliada desembolsa desde su cuenta y devuelve el código del crédito. */
    Result<String, ErrorCredito> confirmarDesembolso(String codigoSolicitud);

    /** CU-11 y CU-13: Kapikua transfiere lo cobrado a la entidad aliada, que actualiza la clasificación del deudor. */
    Result<Void, ErrorCredito> registrarPago(String codigoCredito, Dinero monto);
}
