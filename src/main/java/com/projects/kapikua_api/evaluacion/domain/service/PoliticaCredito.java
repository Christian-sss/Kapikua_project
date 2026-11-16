package com.projects.kapikua_api.evaluacion.domain.service;

import com.projects.kapikua_api.evaluacion.domain.error.ErrorEvaluacion;
import com.projects.kapikua_api.evaluacion.domain.model.ClasificacionSbs;
import com.projects.kapikua_api.evaluacion.domain.model.CobrosDelPeriodo;
import com.projects.kapikua_api.evaluacion.domain.model.CondicionesSugeridas;
import com.projects.kapikua_api.evaluacion.domain.model.ConfiguracionFactoresScore;
import com.projects.kapikua_api.evaluacion.domain.model.ContextoEvaluacion;
import com.projects.kapikua_api.evaluacion.domain.model.ContextoEvaluacion.OfertaViable;
import com.projects.kapikua_api.evaluacion.domain.model.Evaluacion;
import com.projects.kapikua_api.evaluacion.domain.model.FactorEvaluado;
import com.projects.kapikua_api.evaluacion.domain.model.HistorialCrediticio;
import com.projects.kapikua_api.evaluacion.domain.model.InformeCentral;
import com.projects.kapikua_api.evaluacion.domain.model.ParametrosCredito;
import com.projects.kapikua_api.evaluacion.domain.model.SolicitudCredito;
import com.projects.kapikua_api.evaluacion.domain.model.UsoMinimo;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.MatematicaFinanciera;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class PoliticaCredito {
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final List<EstrategiaRecomendacion> ESTRATEGIAS = List.of(
            new RecomendacionNoApto(),
            new RecomendacionApto(),
            new RecomendacionAptoConAjuste());

    private PoliticaCredito() {
    }

    public static Result<Void, ErrorEvaluacion> verificarElegibilidad(
            SolicitudCredito solicitud,
            CobrosDelPeriodo cobros,
            HistorialCrediticio historial,
            ParametrosCredito p
    ) {
        var nivel = p.reglasNivel().nivel(historial.getNivel());
        if (solicitud.monto().esCero()) return Result.err(new ErrorEvaluacion.MontoInvalido());
        var cuotasMinimas = p.limitesCredito().cuotasMinimas();
        if (solicitud.cuotas() < cuotasMinimas || solicitud.cuotas() > nivel.maxCuotas()) {
            return Result.err(new ErrorEvaluacion.CuotasFueraDeRango(cuotasMinimas, nivel.maxCuotas()));
        }
        var falta = faltaUsoMinimo(cobros, p.usoMinimo());
        if (falta != null) return Result.err(new ErrorEvaluacion.BilleteraSinUsoMinimo(falta));
        return Result.ok();
    }

    public static Result<Evaluacion, ErrorEvaluacion> evaluar(
            UsuarioId usuarioId,
            SolicitudCredito solicitud,
            CobrosDelPeriodo cobros,
            InformeCentral central,
            HistorialCrediticio historial,
            ParametrosCredito p,
            OffsetDateTime ahora
    ) {
        var elegibilidad = verificarElegibilidad(solicitud, cobros, historial, p);
        if (elegibilidad instanceof Result.Err<Void, ErrorEvaluacion> err) return Result.err(err.error());

        var limites = p.limitesCredito();
        var reglasScore = p.score();
        var eliminatoria = reglaEliminatoria(solicitud, central, historial, limites.deudaMaxima());
        if (eliminatoria.isPresent()) {
            var contexto = new ContextoEvaluacion(usuarioId, solicitud, central, new Evaluacion.DetalleScore(null, List.of()),
                    reglasScore.recomendacion(), ahora, Optional.empty(), eliminatoria);
            return Result.ok(recomendar(contexto));
        }

        var nivel = p.reglasNivel().nivel(historial.getNivel());
        var tea = nivel.tea().min(limites.tasaMaxima());
        var i = MatematicaFinanciera.tasaSemanal(tea);
        var promedio = cobros.promedioSemanal();
        var cuotaMaxima = promedio.multiply(limites.porcentajeCapacidadPago())
                .divide(CIEN, 10, RoundingMode.HALF_EVEN);
        var capacidadNivel = MatematicaFinanciera.valorPresente(cuotaMaxima, i, nivel.maxCuotas())
                .multiply(nivel.porcentajeCapacidad()).divide(CIEN, 10, RoundingMode.HALF_EVEN);
        var capacidadConSusCuotas = MatematicaFinanciera.valorPresente(cuotaMaxima, i, solicitud.cuotas());
        var montoPosible = Stream.of(solicitud.monto().valor(), capacidadNivel, capacidadConSusCuotas,
                        limites.topePorOperacion().valor())
                .min(BigDecimal::compareTo).orElseThrow()
                .setScale(2, RoundingMode.DOWN);

        var cuotaPedida = MatematicaFinanciera.cuota(solicitud.monto(), i, solicitud.cuotas());
        var factores = factores(cobros, central, historial, cuotaPedida, cuotaMaxima, reglasScore.factores());
        var score = factores.stream().map(FactorEvaluado::aporte).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP).intValueExact();

        var reglas = reglasScore.recomendacion();
        if (score >= reglas.scoreAjuste()
                && ContextoEvaluacion.requiereAjustePorRiesgo(score, central.clasificacion(), reglas)) {
            var montoAjustado = solicitud.monto().valor().multiply(reglas.porcentajeAjuste())
                    .divide(CIEN, 2, RoundingMode.DOWN);
            montoPosible = montoPosible.min(montoAjustado);
        }
        var monto = Dinero.de(montoPosible);
        var cuota = MatematicaFinanciera.cuota(monto, i, solicitud.cuotas());
        var condiciones = new CondicionesSugeridas(monto, solicitud.cuotas(), tea, cuota,
                p.retencion().porcentaje(cuota, promedio));
        var requiereAval = historial.getNivel() <= p.reglasNivel().nivelMaxConAval();
        var oferta = new OfertaViable(condiciones, limites.cuotaMinima(), requiereAval);
        var contexto = new ContextoEvaluacion(usuarioId, solicitud, central, new Evaluacion.DetalleScore(score, factores),
                reglas, ahora, Optional.of(oferta), Optional.empty());
        return Result.ok(recomendar(contexto));
    }

    private static Evaluacion recomendar(ContextoEvaluacion contexto) {
        return ESTRATEGIAS.stream()
                .filter(estrategia -> estrategia.aplica(contexto))
                .findFirst().orElseThrow().evaluar(contexto);
    }

    public static Result<Void, ErrorEvaluacion> evaluarAval(boolean identidadValidada, InformeCentral central,
                                                            HistorialCrediticio historial) {
        if (!identidadValidada) return Result.err(new ErrorEvaluacion.AvalIdentidadNoValidada());
        if (central.clasificacion() != ClasificacionSbs.NORMAL)
            return Result.err(new ErrorEvaluacion.AvalClasificacionNoNormal());
        if (historial.isBloqueadoInterno()) return Result.err(new ErrorEvaluacion.AvalBloqueado());
        return Result.ok();
    }

    private static String faltaUsoMinimo(CobrosDelPeriodo c, UsoMinimo p) {
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

    private static Optional<String> reglaEliminatoria(SolicitudCredito s, InformeCentral central, HistorialCrediticio h,
                                                       Dinero deudaMaxima) {
        if (h.isBloqueadoInterno()) return Optional.of("Tienes un bloqueo interno por atrasos en créditos anteriores.");
        if (central.clasificacion() != null && central.clasificacion().esEliminatoria()) {
            return Optional.of("Tu clasificación en el sistema financiero es " + central.clasificacion() + ".");
        }
        if (central.deudaTotal().sumar(s.monto()).mayorQue(deudaMaxima)) {
            return Optional.of("Tu deuda total en el sistema financiero más este crédito superaría " + deudaMaxima + ".");
        }
        return Optional.empty();
    }

    private static List<FactorEvaluado> factores(CobrosDelPeriodo c, InformeCentral central, HistorialCrediticio h,
                                                 Dinero cuotaPedida, BigDecimal cuotaMaxima,
                                                 ConfiguracionFactoresScore configuracion) {
        var ponderaciones = configuracion.ponderaciones();
        var referencias = configuracion.referencias();
        var puntajesSbs = configuracion.puntajesSbs();
        var regularidad = c.semanas() == 0 ? BigDecimal.ZERO
                : porcentaje(BigDecimal.valueOf(c.semanasConCobros()), BigDecimal.valueOf(c.semanas()));
        var holgura = cuotaMaxima.signum() == 0 ? BigDecimal.ZERO
                : acotar(CIEN.subtract(porcentaje(cuotaPedida.valor(), cuotaMaxima)));
        var historial = h.esNuevo() ? referencias.historialNuevo()
                : acotar(CIEN.subtract(referencias.historialMenosPorDia()
                        .multiply(BigDecimal.valueOf(h.getMaxDiasAtraso()))));
        var pagadores = acotar(porcentaje(BigDecimal.valueOf(c.pagadoresDistintos()),
                BigDecimal.valueOf(referencias.refPagadores())));
        var antiguedad = acotar(porcentaje(BigDecimal.valueOf(c.diasAntiguedad()),
                BigDecimal.valueOf(referencias.refAntiguedadDias())));
        var clasificacion = central.clasificacion() == null ? puntajesSbs.sinHistorial()
                : switch (central.clasificacion()) {
            case NORMAL -> puntajesSbs.normal();
            case CPP -> puntajesSbs.cpp();
            default -> BigDecimal.ZERO;
        };
        return List.of(
                new FactorEvaluado("regularidad_cobros", c.semanasConCobros() + "/" + c.semanas() + " semanas",
                        redondear(regularidad), ponderaciones.regularidad()),
                new FactorEvaluado("holgura_cuota", cuotaPedida + " de " + Dinero.de(cuotaMaxima) + " posibles",
                        redondear(holgura), ponderaciones.holgura()),
                new FactorEvaluado("historial_kapikua", h.esNuevo() ? "sin créditos previos"
                        : h.getMaxDiasAtraso() + " días de mayor atraso", redondear(historial), ponderaciones.historial()),
                new FactorEvaluado("pagadores_distintos", String.valueOf(c.pagadoresDistintos()),
                        redondear(pagadores), ponderaciones.pagadores()),
                new FactorEvaluado("antiguedad_billetera", c.diasAntiguedad() + " días",
                        redondear(antiguedad), ponderaciones.antiguedad()),
                new FactorEvaluado("clasificacion_sbs", central.clasificacion() == null ? "sin historial"
                        : central.clasificacion().name(), redondear(clasificacion), ponderaciones.clasificacion()));
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
