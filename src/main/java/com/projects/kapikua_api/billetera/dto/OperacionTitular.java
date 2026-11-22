package com.projects.kapikua_api.billetera.dto;

import com.projects.kapikua_api.shared.Dinero;

import java.time.OffsetDateTime;

public record OperacionTitular(Long transaccionId, String tipo, Dinero monto, OffsetDateTime fecha) {
}
