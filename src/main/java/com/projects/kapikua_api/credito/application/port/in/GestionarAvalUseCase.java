package com.projects.kapikua_api.credito.application.port.in;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public interface GestionarAvalUseCase {
    Result<Void, ErrorCredito> designar(UsuarioId deudor, Long ofertaId, String celularAval);

    Result<InvitacionAval, ErrorCredito> invitacion(UsuarioId aval, Long ofertaId);

    Result<Void, ErrorCredito> responder(UsuarioId aval, Long ofertaId, boolean acepta, String codigo, String dispositivo);

    record InvitacionAval(Long ofertaId, String deudor, Dinero monto, int cuotas, Dinero cuota, BigDecimal tea,
                          int porcentajeRetencion, OffsetDateTime vence, String codigoMostrado) {}
}
