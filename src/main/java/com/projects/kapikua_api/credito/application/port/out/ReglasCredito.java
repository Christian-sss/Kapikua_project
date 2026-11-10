package com.projects.kapikua_api.credito.application.port.out;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;

/** Parámetros vigentes de la originación (RN-17, RN-19, RN-21), leídos de Cumplimiento. */
public interface ReglasCredito {

    /**
     * @param tasaMoratoriaMaxima anual, en porcentaje (RN-24); va en la hoja resumen y se cobra por atraso
     * @param diasAvisoAval       RN-25: días de atraso para avisar al aval (9)
     * @param diasActivacionAval  RN-25: días de atraso para retener las ventas del aval (15)
     */
    record Reglas(int vigenciaOfertaDias, Duration plazoRespuestaAval, int maxAvales, Dinero cuotaMinima,
                  Dinero topePorOperacion, BigDecimal tasaMoratoriaMaxima, int diasAvisoAval, int diasActivacionAval) {}

    Result<Reglas, ErrorCredito> vigentes(LocalDate fecha);
}
