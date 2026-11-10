package com.projects.kapikua_api.cumplimiento.application.port.in;

import com.projects.kapikua_api.cumplimiento.domain.error.ParametroError;
import com.projects.kapikua_api.shared.Result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** Para los demás contextos: el valor de un parámetro vigente en una fecha (RNF-12). */
public interface ConsultarParametrosUseCase {

    Result<BigDecimal, ParametroError> valorVigente(String codigo, LocalDate fecha);

    /** Todos los parámetros vigentes en la fecha, en una sola consulta (código → valor). */
    Map<String, BigDecimal> vigentes(LocalDate fecha);
}
