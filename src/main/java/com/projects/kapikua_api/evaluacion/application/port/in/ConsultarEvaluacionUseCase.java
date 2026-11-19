package com.projects.kapikua_api.evaluacion.application.port.in;

import com.projects.kapikua_api.evaluacion.domain.error.ErrorEvaluacion;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

import java.math.BigDecimal;
import java.util.Optional;

public interface ConsultarEvaluacionUseCase {
    Optional<ResultadoEvaluacion> porId(Long evaluacionId);

    Result<NivelCliente, ErrorEvaluacion> nivel(UsuarioId usuarioId);

    record NivelCliente(int nivel, int nivelMaximo, BigDecimal porcentajeCapacidad, int cuotasMinimas, int maxCuotas,
                        BigDecimal tea, int creditosPagadosEnNivel, int creditosParaSubir, boolean requiereAval,
                        boolean bloqueado, UsoMinimo usoMinimo, Siguiente siguiente) {}

    record UsoMinimo(long dias, int diasRequeridos, int semanasConCobros, int semanasRequeridas,
                     int cobros, int cobrosRequeridos, Dinero promedioSemanal) {
        public boolean cumple() {
            return dias >= diasRequeridos && semanasConCobros >= semanasRequeridas && cobros >= cobrosRequeridos;
        }
    }

    record Siguiente(int nivel, BigDecimal porcentajeCapacidad, int maxCuotas, BigDecimal tea, boolean requiereAval) {}
}
