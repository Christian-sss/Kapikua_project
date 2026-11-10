package com.projects.kapikua_api.billetera.domain.event;

import com.projects.kapikua_api.billetera.domain.model.TipoTransaccion;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.domain.EventoDominio;

import java.time.OffsetDateTime;

public record OperacionRegistrada(Long transaccionId, TipoTransaccion tipo, Dinero monto, Long billeteraOrigenId,
                                  Long billeteraDestinoId, OffsetDateTime fecha) implements EventoDominio {}
