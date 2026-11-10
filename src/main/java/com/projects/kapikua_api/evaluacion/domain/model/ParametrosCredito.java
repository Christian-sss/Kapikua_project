package com.projects.kapikua_api.evaluacion.domain.model;

import com.projects.kapikua_api.shared.Dinero;

import java.math.BigDecimal;
import java.util.Map;

public record ParametrosCredito(
        int usoMinDias, int usoMinSemanasConCobro, int usoMinCobros,
        BigDecimal porcentajeCapacidadPago, Dinero deudaMaxima, Dinero topePorOperacion, BigDecimal tasaMaxima,
        Dinero cuotaMinima, int cuotasMinimas, Map<Integer, CondicionesNivel> niveles,
        int scoreApto, int scoreAjuste, BigDecimal porcentajeAjuste,
        PesosScore pesos,
        int retencionMin, int retencionMax, int retencionDefecto, int nivelMaxConAval,
        int diasAtrasoMaxParaSubir, int diasAtrasoBajaNivel, int diasAtrasoBloqueo,
        int periodoCobrosSemanas) {
    public record PesosScore(BigDecimal regularidad, BigDecimal holgura, BigDecimal historial,
                             BigDecimal pagadores, BigDecimal antiguedad, BigDecimal clasificacion,
                             int refPagadores, int refAntiguedadDias,
                             BigDecimal historialNuevo, BigDecimal historialMenosPorDia,
                             BigDecimal sbsNormal, BigDecimal sbsSinHistorial, BigDecimal sbsCpp) {}

    public CondicionesNivel nivel(int nivel) {
        var c = niveles.get(nivel);
        if (c == null) throw new IllegalArgumentException("Nivel sin parámetros: " + nivel);
        return c;
    }

    public int nivelMaximo() {
        return niveles.keySet().stream().mapToInt(Integer::intValue).max().orElse(1);
    }
}
