package com.projects.kapikua_api.credito.domain.event;

import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.domain.EventoDominio;

/** CU-13: el préstamo sigue con cuotas vencidas; {@code dias} es el atraso de la más antigua (RN-26). */
public record AtrasoDetectado(UsuarioId usuarioId, Long prestamoId, Long ofertaId, int dias) implements EventoDominio {}
