package com.projects.kapikua_api.cumplimiento.application.port.in;

import com.projects.kapikua_api.cumplimiento.domain.error.ParametroError;
import com.projects.kapikua_api.shared.Result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public interface ConsultarParametrosUseCase {
    Result<BigDecimal, ParametroError> valorVigente(String codigo, LocalDate fecha);

    Map<String, BigDecimal> vigentes(LocalDate fecha);
}
