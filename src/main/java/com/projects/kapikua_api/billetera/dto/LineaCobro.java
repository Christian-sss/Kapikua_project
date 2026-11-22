package com.projects.kapikua_api.billetera.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record LineaCobro(OffsetDateTime fecha, BigDecimal monto, Long billeteraPagadora) {
}
