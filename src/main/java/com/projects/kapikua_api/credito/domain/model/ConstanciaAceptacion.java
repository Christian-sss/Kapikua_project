package com.projects.kapikua_api.credito.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;

/** RN-20: fecha, hora, dispositivo y versión de la hoja resumen que vio el cliente al aceptar. */
public record ConstanciaAceptacion(OffsetDateTime fecha, String dispositivo, String versionHojaResumen) {

    public ConstanciaAceptacion {
        Objects.requireNonNull(fecha);
        if (dispositivo == null || dispositivo.isBlank()) dispositivo = "desconocido";
        if (dispositivo.length() > 150) dispositivo = dispositivo.substring(0, 150);
        Objects.requireNonNull(versionHojaResumen);
    }
}
