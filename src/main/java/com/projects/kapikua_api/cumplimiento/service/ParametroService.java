package com.projects.kapikua_api.cumplimiento.service;

import com.projects.kapikua_api.cumplimiento.error.ParametroError;
import com.projects.kapikua_api.cumplimiento.model.Parametro;
import com.projects.kapikua_api.cumplimiento.repository.ParametroRepository;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

/** Parámetros regulatorios vigentes (UIT, topes SBS, tasas). Los usan también Billetera, Evaluación y Crédito. */
@Service
public class ParametroService {

    private final ParametroRepository parametros;

    public ParametroService(ParametroRepository parametros) {
        this.parametros = parametros;
    }

    public Result<BigDecimal, ParametroError> valorVigente(String codigo, LocalDate fecha) {
        return parametros.vigente(codigo, fecha)
                .map(Parametro::getValor)
                .<Result<BigDecimal, ParametroError>>map(Result::ok)
                .orElseGet(() -> Result.err(new ParametroError.NoVigente(codigo, fecha)));
    }

    public Map<String, BigDecimal> vigentes(LocalDate fecha) {
        return parametros.vigentes(fecha).stream()
                .collect(Collectors.toUnmodifiableMap(Parametro::getCodigo, Parametro::getValor));
    }

    /** Tope por operación: UIT × TOPE_OPERACION_UIT. Sin parámetros vigentes no se puede operar. */
    public Dinero topePorOperacion(LocalDate fecha) {
        return Dinero.de(valor("UIT", fecha).multiply(valor("TOPE_OPERACION_UIT", fecha)));
    }

    private BigDecimal valor(String codigo, LocalDate fecha) {
        return switch (valorVigente(codigo, fecha)) {
            case Result.Ok<BigDecimal, ParametroError> ok -> ok.valor();
            case Result.Err<BigDecimal, ParametroError> err -> throw new IllegalStateException(err.error().mensaje());
        };
    }
}
