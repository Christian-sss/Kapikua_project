package com.projects.kapikua_api.billetera.service;

import com.projects.kapikua_api.billetera.error.ErrorBilletera;
import com.projects.kapikua_api.billetera.model.Topes;
import com.projects.kapikua_api.cumplimiento.error.ParametroError;
import com.projects.kapikua_api.cumplimiento.service.ParametroService;
import com.projects.kapikua_api.shared.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Topes SBS vigentes (por operación, saldo máximo y pagos del mes), calculados con los parámetros de Cumplimiento. */
@Service
public class TopesService {
    private static final Logger log = LoggerFactory.getLogger(TopesService.class);
    private static final List<String> CODIGOS = List.of(
            "UIT", "TOPE_OPERACION_UIT", "TOPE_SALDO_UIT", "TOPE_PAGOS_MES_UIT");

    private final ParametroService parametros;

    public TopesService(ParametroService parametros) {
        this.parametros = parametros;
    }

    public Result<Topes, ErrorBilletera> topesVigentes(LocalDate fecha) {
        var valores = new ArrayList<BigDecimal>();
        for (var codigo : CODIGOS) {
            switch (parametros.valorVigente(codigo, fecha)) {
                case Result.Ok<BigDecimal, ParametroError> ok -> valores.add(ok.valor());
                case Result.Err<BigDecimal, ParametroError> err -> {
                    log.error("Configuración incompleta: {}", err.error().mensaje());
                    return Result.err(new ErrorBilletera.ParametrosNoDisponibles());
                }
            }
        }
        return Result.ok(Topes.desdeUit(valores.get(0), valores.get(1), valores.get(2), valores.get(3)));
    }
}
