package com.projects.kapikua_api.billetera.infrastructure.adapter.out.contexto;

import com.projects.kapikua_api.billetera.application.port.out.ParametrosRegulatorios;
import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.billetera.domain.model.Topes;
import com.projects.kapikua_api.cumplimiento.application.port.in.ConsultarParametrosUseCase;
import com.projects.kapikua_api.cumplimiento.domain.error.ParametroError;
import com.projects.kapikua_api.shared.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Lee los topes vigentes de Cumplimiento (códigos del script v6, Res. SBS 00704-2026). */
@Component
class ParametrosRegulatoriosAdapter implements ParametrosRegulatorios {

    private static final Logger log = LoggerFactory.getLogger(ParametrosRegulatoriosAdapter.class);
    private static final List<String> CODIGOS = List.of(
            "UIT", "TOPE_OPERACION_UIT", "TOPE_SALDO_UIT", "TOPE_RECARGAS_MES_UIT", "TOPE_PAGOS_MES_UIT");

    private final ConsultarParametrosUseCase parametros;

    ParametrosRegulatoriosAdapter(ConsultarParametrosUseCase parametros) {
        this.parametros = parametros;
    }

    @Override
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
        return Result.ok(Topes.desdeUit(valores.get(0), valores.get(1), valores.get(2), valores.get(3), valores.get(4)));
    }
}
