package com.projects.kapikua_api.credito.domain.model;

import com.projects.kapikua_api.shared.Dinero;

import java.time.OffsetDateTime;

public record Retencion(Long cuotaId, Long transaccionVentaId, Origen origen, Dinero monto, OffsetDateTime fecha) {}
