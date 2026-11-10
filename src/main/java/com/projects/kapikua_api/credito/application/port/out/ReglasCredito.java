package com.projects.kapikua_api.credito.application.port.out;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;

public interface ReglasCredito {
    record Reglas(int vigenciaOfertaDias, Duration plazoRespuestaAval, int maxAvales, Dinero cuotaMinima,
                  Dinero topePorOperacion, BigDecimal tasaMoratoriaMaxima, int diasAvisoAval, int diasActivacionAval) {}

    Result<Reglas, ErrorCredito> vigentes(LocalDate fecha);
}
