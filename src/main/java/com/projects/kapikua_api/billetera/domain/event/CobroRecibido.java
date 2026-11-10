package com.projects.kapikua_api.billetera.domain.event;

import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.domain.EventoDominio;

public record CobroRecibido(Long billeteraId, Long transaccionId, Dinero monto) implements EventoDominio {}
