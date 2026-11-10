package com.projects.kapikua_api.billetera.domain.event;

import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.domain.EventoDominio;

/** Una billetera recibió dinero de otra (pago a negocio o transferencia): es un cobro (RN-08, RN-22). */
public record CobroRecibido(Long billeteraId, Long transaccionId, Dinero monto) implements EventoDominio {}
