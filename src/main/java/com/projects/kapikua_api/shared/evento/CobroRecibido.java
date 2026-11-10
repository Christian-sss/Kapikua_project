package com.projects.kapikua_api.shared.evento;

import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.UsuarioId;

/**
 * Evento de integración: un usuario recibió un pago a negocio o una transferencia.
 * Créditos lo escuchará en la misma transacción para aplicar la retención de ventas (RN-22).
 */
public record CobroRecibido(UsuarioId usuarioId, Long billeteraId, Long transaccionId, Dinero monto) {}
