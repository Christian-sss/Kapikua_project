package com.projects.kapikua_api.evaluacion.domain.model;

import com.projects.kapikua_api.shared.Dinero;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Política de crédito vigente, leída de cumplimiento.parametro (RNF-12). Ningún valor está fijo en el código.
 */
public record ParametrosCredito(
        // Elegibilidad (RN-08)
        int usoMinDias, int usoMinSemanasConCobro, int usoMinCobros,
        // Capacidad, monto y cuotas (RN-11 a RN-17)
        BigDecimal porcentajeCapacidadPago, Dinero deudaMaxima, Dinero topePorOperacion, BigDecimal tasaMaxima,
        Dinero cuotaMinima, int cuotasMinimas, Map<Integer, CondicionesNivel> niveles,
        // Recomendación (RN-15)
        int scoreApto, int scoreAjuste, BigDecimal porcentajeAjuste,
        PesosScore pesos,
        // Retención (RN-22) y aval (RN-21)
        int retencionMin, int retencionMax, int retencionDefecto, int nivelMaxConAval,
        // Escalera (RN-26)
        int diasAtrasoMaxParaSubir, int diasAtrasoBajaNivel, int diasAtrasoBloqueo,
        // Semanas completas que se promedian para la capacidad de pago (RN-12)
        int periodoCobrosSemanas) {

    /** Pesos y referencias del score (RN-15). Los pesos suman 1000. */
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
