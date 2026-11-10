package com.projects.kapikua_api.identidad.domain.model;

/** Datos que RENIEC devuelve al confirmar un DNI con su fecha de nacimiento. */
public record IdentidadVerificada(String nombres, String apellidos) {}
