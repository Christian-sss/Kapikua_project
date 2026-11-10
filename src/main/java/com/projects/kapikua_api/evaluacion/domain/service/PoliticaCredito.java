package com.projects.kapikua_api.evaluacion.domain.service;

import com.projects.kapikua_api.evaluacion.domain.model.*;

import com.projects.kapikua_api.evaluacion.domain.error.ErrorEvaluacion;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.MatematicaFinanciera;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class PoliticaCredito {
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private PoliticaCredito() {
    }

    public static Result<Void, ErrorEvaluacion> verificarElegibilidad(SolicitudCredito solicitud, CobrosDelPeriodo cobros,
                                                                      HistorialCrediticio historial, ParametrosCredito p) {
        var nivel = p.nivel(historial.getNivel());
        if (solicitud.monto().esCero()) return Result.err(new ErrorEvaluacion.MontoInvalido());
        if (solicitud.cuotas() < p.cuotasMinimas() || solicitud.cuotas() > nivel.maxCuotas()) {
            return Result.err(new ErrorEvaluacion.CuotasFueraDeRango(p.cuotasMinimas(), nivel.maxCuotas()));
        }
        var falta = faltaUsoMinimo(cobros, p);
        if (falta != null) return Result.err(new ErrorEvaluacion.BilleteraSinUsoMinimo(falta));
        return Result.ok();
    }

    public static Result<Evaluacion, ErrorEvaluacion> evaluar(UsuarioId usuarioId, SolicitudCredito solicitud,
                                                              CobrosDelPeriodo cobros, InformeCentral central,
                                                              HistorialCrediticio historial, ParametrosCredito p,
                                                              OffsetDateTime ahora) {
        var nivel = p.nivel(historial.getNivel());

        var elegibilidad = verificarElegibilidad(solicitud, cobros, historial, p);
        if (elegibilidad instanceof Result.Err<Void, ErrorEvaluacion> err) return Result.err(err.error());

        var eliminatoria = reglaEliminatoria(solicitud, central, historial, p);
        if (eliminatoria != null) {
            return Result.ok(Evaluacion.noApto(usuarioId, solicitud, central, null, eliminatoria, List.of(), ahora));
        }

        var tea = nivel.tea().min(p.tasaMaxima());
        var i = MatematicaFinanciera.tasaSemanal(tea);
        var promedio = cobros.promedioSemanal();
        var cuotaMaxima = promedio.multiply(p.porcentajeCapacidadPago()).divide(CIEN, 10, RoundingMode.HALF_EVEN);
        var capacidadNivel = MatematicaFinanciera.valorPresente(cuotaMaxima, i, nivel.maxCuotas())
                .multiply(nivel.porcentajeCapacidad()).divide(CIEN, 10, RoundingMode.HALF_EVEN);
        var capacidadConSusCuotas = MatematicaFinanciera.valorPresente(cuotaMaxima, i, solicitud.cuotas());
        var montoPosible = Stream.of(solicitud.monto().valor(), capacidadNivel, capacidadConSusCuotas,
                        p.topePorOperacion().valor())
                .min(BigDecimal::compareTo).orElseThrow()
                .setScale(2, RoundingMode.DOWN);

        var cuotaPedida = MatematicaFinanciera.cuota(solicitud.monto(), i, solicitud.cuotas());
        var factores = factores(cobros, central, historial, cuotaPedida, cuotaMaxima, p.pesos());
        var score = factores.stream().map(FactorEvaluado::aporte).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP).intValueExact();

        if (score < p.scoreAjuste()) {
            return Result.ok(Evaluacion.noApto(usuarioId, solicitud, central, score,
                    "Score de " + score + ", menor que " + p.scoreAjuste() + ".", factores, ahora));
        }
        var recomendacion = Recomendacion.APTO;
        String motivo = null;
        var monto = montoPosible;
        if (score < p.scoreApto()) {
            recomendacion = Recomendacion.APTO_CON_AJUSTE;
            motivo = "Score de " + score + ": se ofrece hasta el " + p.porcentajeAjuste().stripTrailingZeros().toPlainString()
                    + " % de lo pedido.";
        } else if (central.clasificacion() == ClasificacionSbs.CPP) {
            recomendacion = Recomendacion.APTO_CON_AJUSTE;
            motivo = "Clasificación CPP en el sistema financiero: se ofrece hasta el "
                    + p.porcentajeAjuste().stripTrailingZeros().toPlainString() + " % de lo pedido.";
        }
        if (recomendacion == Recomendacion.APTO_CON_AJUSTE) {
            var ajustado = solicitud.monto().valor().multiply(p.porcentajeAjuste()).divide(CIEN, 2, RoundingMode.DOWN);
            monto = monto.min(ajustado);
        }

        var montoOfrecido = Dinero.de(monto);
        var cuota = montoOfrecido.esCero() ? Dinero.CERO : MatematicaFinanciera.cuota(montoOfrecido, i, solicitud.cuotas());
        if (cuota.menorQue(p.cuotaMinima())) {
            return Result.ok(Evaluacion.noApto(usuarioId, solicitud, central, score,
                    "Tu capacidad de pago permite una cuota de " + cuota + ", menor que la cuota mínima de "
                            + p.cuotaMinima() + ".", factores, ahora));
        }
        var condiciones = new CondicionesSugeridas(montoOfrecido, solicitud.cuotas(), tea, cuota,
                porcentajeRetencion(cuota, promedio, p));
        var requiereAval = historial.getNivel() <= p.nivelMaxConAval();
        return Result.ok(Evaluacion.emitir(usuarioId, solicitud, central, score, recomendacion, condiciones,
                requiereAval, motivo, factores, ahora));
    }

    public static Result<Void, ErrorEvaluacion> evaluarAval(boolean identidadValidada, InformeCentral central,
                                                            HistorialCrediticio historial) {
        if (!identidadValidada) return Result.err(new ErrorEvaluacion.AvalIdentidadNoValidada());
        if (central.clasificacion() != ClasificacionSbs.NORMAL)
            return Result.err(new ErrorEvaluacion.AvalClasificacionNoNormal());
        if (historial.isBloqueadoInterno()) return Result.err(new ErrorEvaluacion.AvalBloqueado());
        return Result.ok();
    }

    private static String faltaUsoMinimo(CobrosDelPeriodo c, ParametrosCredito p) {
        var faltas = new ArrayList<String>();
        if (c.diasAntiguedad() < p.usoMinDias()) {
            faltas.add("tu billetera debe tener al menos " + p.usoMinDias() + " días (tiene " + c.diasAntiguedad() + ")");
        }
        if (c.semanasConCobros() < p.usoMinSemanasConCobro()) {
            faltas.add("necesitas cobros en al menos " + p.usoMinSemanasConCobro() + " semanas (tienes "
                    + c.semanasConCobros() + ")");
        }
        if (c.cantidadCobros() < p.usoMinCobros()) {
            faltas.add("necesitas al menos " + p.usoMinCobros() + " cobros recibidos (tienes " + c.cantidadCobros() + ")");
        }
        return faltas.isEmpty() ? null : String.join("; ", faltas);
    }

    private static String reglaEliminatoria(SolicitudCredito s, InformeCentral central, HistorialCrediticio h,
                                            ParametrosCredito p) {
        if (h.isBloqueadoInterno()) return "Tienes un bloqueo interno por atrasos en créditos anteriores.";
        if (central.clasificacion() != null && central.clasificacion().esEliminatoria()) {
            return "Tu clasificación en el sistema financiero es " + central.clasificacion() + ".";
        }
        if (central.deudaTotal().sumar(s.monto()).mayorQue(p.deudaMaxima())) {
            return "Tu deuda total en el sistema financiero más este crédito superaría " + p.deudaMaxima() + ".";
        }
        return null;
    }

    private static List<FactorEvaluado> factores(CobrosDelPeriodo c, InformeCentral central, HistorialCrediticio h,
                                                 Dinero cuotaPedida, BigDecimal cuotaMaxima,
                                                 ParametrosCredito.PesosScore w) {
        var regularidad = c.semanas() == 0 ? BigDecimal.ZERO
                : porcentaje(BigDecimal.valueOf(c.semanasConCobros()), BigDecimal.valueOf(c.semanas()));
        var holgura = cuotaMaxima.signum() == 0 ? BigDecimal.ZERO
                : acotar(CIEN.subtract(porcentaje(cuotaPedida.valor(), cuotaMaxima)));
        var historial = h.esNuevo() ? w.historialNuevo()
                : acotar(CIEN.subtract(w.historialMenosPorDia().multiply(BigDecimal.valueOf(h.getMaxDiasAtraso()))));
        var pagadores = acotar(porcentaje(BigDecimal.valueOf(c.pagadoresDistintos()), BigDecimal.valueOf(w.refPagadores())));
        var antiguedad = acotar(porcentaje(BigDecimal.valueOf(c.diasAntiguedad()), BigDecimal.valueOf(w.refAntiguedadDias())));
        var clasificacion = central.clasificacion() == null ? w.sbsSinHistorial()
                : switch (central.clasificacion()) {
            case NORMAL -> w.sbsNormal();
            case CPP -> w.sbsCpp();
            default -> BigDecimal.ZERO;
        };
        return List.of(
                new FactorEvaluado("regularidad_cobros", c.semanasConCobros() + "/" + c.semanas() + " semanas",
                        redondear(regularidad), w.regularidad()),
                new FactorEvaluado("holgura_cuota", cuotaPedida + " de " + Dinero.de(cuotaMaxima) + " posibles",
                        redondear(holgura), w.holgura()),
                new FactorEvaluado("historial_kapikua", h.esNuevo() ? "sin créditos previos"
                        : h.getMaxDiasAtraso() + " días de mayor atraso", redondear(historial), w.historial()),
                new FactorEvaluado("pagadores_distintos", String.valueOf(c.pagadoresDistintos()),
                        redondear(pagadores), w.pagadores()),
                new FactorEvaluado("antiguedad_billetera", c.diasAntiguedad() + " días",
                        redondear(antiguedad), w.antiguedad()),
                new FactorEvaluado("clasificacion_sbs", central.clasificacion() == null ? "sin historial"
                        : central.clasificacion().name(), redondear(clasificacion), w.clasificacion()));
    }

    private static int porcentajeRetencion(Dinero cuota, BigDecimal promedioSemanal, ParametrosCredito p) {
        if (promedioSemanal.signum() == 0) return p.retencionDefecto();
        var porcentaje = cuota.valor().multiply(CIEN).divide(promedioSemanal, 0, RoundingMode.CEILING).intValue();
        return Math.max(p.retencionMin(), Math.min(p.retencionMax(), porcentaje));
    }

    private static BigDecimal porcentaje(BigDecimal parte, BigDecimal total) {
        return parte.multiply(CIEN).divide(total, 10, RoundingMode.HALF_EVEN);
    }

    private static BigDecimal acotar(BigDecimal puntaje) {
        return puntaje.max(BigDecimal.ZERO).min(CIEN);
    }

    private static BigDecimal redondear(BigDecimal puntaje) {
        return puntaje.setScale(2, RoundingMode.HALF_EVEN);
    }
}
