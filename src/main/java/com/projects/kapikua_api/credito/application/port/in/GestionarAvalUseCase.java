package com.projects.kapikua_api.credito.application.port.in;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** CU-09 Gestionar aval (RF-CR-10, RN-21). */
public interface GestionarAvalUseCase {

    /** El deudor designa a su aval por el celular. */
    Result<Void, ErrorCredito> designar(UsuarioId deudor, Long ofertaId, String celularAval);

    /** Lo que ve el aval antes de responder; genera su código de un solo uso (RNF-04). */
    Result<InvitacionAval, ErrorCredito> invitacion(UsuarioId aval, Long ofertaId);

    /** Aceptar exige el código y el consentimiento de garantía; rechazar, no. */
    Result<Void, ErrorCredito> responder(UsuarioId aval, Long ofertaId, boolean acepta, String codigo, String dispositivo);

    /**
     * @param codigoMostrado en la v1 no hay SMS: el código se muestra en pantalla
     */
    record InvitacionAval(Long ofertaId, String deudor, Dinero monto, int cuotas, Dinero cuota, BigDecimal tea,
                          int porcentajeRetencion, OffsetDateTime vence, String codigoMostrado) {}
}
