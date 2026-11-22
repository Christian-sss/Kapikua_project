package com.projects.kapikua_api.credito.dto;

import java.time.OffsetDateTime;

/** Un aval de la oferta, como lo ve el deudor. */
public record AvalVista(String nombre, String estado, OffsetDateTime vence) {
}
