package com.projects.kapikua_api.credito.domain.event;

import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.domain.EventoDominio;

/**
 * CU-10: se desembolsó un préstamo. Se registra al crearlo, antes de que la base le asigne un id, por eso
 * se identifica con el código del crédito de la entidad aliada (su identidad de negocio).
 */
public record PrestamoDesembolsado(UsuarioId usuarioId, Long ofertaId, String codigoCredito, Dinero monto)
        implements EventoDominio {}
