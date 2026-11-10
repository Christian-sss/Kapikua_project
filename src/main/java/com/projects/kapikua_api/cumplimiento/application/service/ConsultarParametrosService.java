package com.projects.kapikua_api.cumplimiento.application.service;

import com.projects.kapikua_api.cumplimiento.application.port.in.ConsultarParametrosUseCase;
import com.projects.kapikua_api.cumplimiento.application.port.out.ParametroRepositoryPort;
import com.projects.kapikua_api.cumplimiento.domain.error.ParametroError;
import com.projects.kapikua_api.cumplimiento.domain.model.Parametro;
import com.projects.kapikua_api.shared.Result;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ConsultarParametrosService implements ConsultarParametrosUseCase {

    private final ParametroRepositoryPort parametros;

    public ConsultarParametrosService(ParametroRepositoryPort parametros) {
        this.parametros = parametros;
    }

    @Override
    public Result<BigDecimal, ParametroError> valorVigente(String codigo, LocalDate fecha) {
        return parametros.vigente(codigo, fecha)
                .map(Parametro::getValor)
                .<Result<BigDecimal, ParametroError>>map(Result::ok)
                .orElseGet(() -> Result.err(new ParametroError.NoVigente(codigo, fecha)));
    }

    @Override
    public Map<String, BigDecimal> vigentes(LocalDate fecha) {
        return parametros.vigentes(fecha).stream()
                .collect(Collectors.toUnmodifiableMap(Parametro::getCodigo, Parametro::getValor));
    }
}
