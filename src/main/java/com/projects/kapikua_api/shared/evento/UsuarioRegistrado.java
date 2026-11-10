package com.projects.kapikua_api.shared.evento;

import com.projects.kapikua_api.shared.UsuarioId;

/**
 * Evento de integración: un cliente terminó su registro.
 * Billetera lo escuchará en la misma transacción para abrir la billetera (CU-01, paso 8).
 */
public record UsuarioRegistrado(UsuarioId usuarioId, String celular, String nombreNegocio) {}
