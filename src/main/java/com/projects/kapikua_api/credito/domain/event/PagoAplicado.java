package com.projects.kapikua_api.credito.domain.event;

import com.projects.kapikua_api.credito.domain.model.Origen;
import com.projects.kapikua_api.credito.domain.model.TipoPago;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.domain.EventoDominio;

public record PagoAplicado(UsuarioId usuarioId, Long prestamoId, String codigoCredito, TipoPago tipo, Origen origen,
                           Dinero monto) implements EventoDominio {}
