package com.projects.kapikua_api.identidad.domain.event;

import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.domain.EventoDominio;

/** CU-01: se registró un cliente. Lleva el celular y el negocio que declaró para abrir su billetera. */
public record UsuarioRegistrado(UsuarioId usuarioId, String celular, String nombreNegocio) implements EventoDominio {}
