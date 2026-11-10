package com.projects.kapikua_api.evaluacion.domain.service;

import com.projects.kapikua_api.evaluacion.domain.model.*;

import com.projects.kapikua_api.evaluacion.domain.error.ErrorEvaluacion;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** RN-08 a RN-17 con los parámetros del script v6 y del cambio 002. */
public class PoliticaCreditoTest {

    public static final UsuarioId CLIENTE = new UsuarioId(1L);
    static final OffsetDateTime AHORA = OffsetDateTime.of(2026, 11, 9, 10, 0, 0, 0, ZoneOffset.ofHours(-5));
    static final InformeCentral SIN_HISTORIAL = new InformeCentral(null, Dinero.CERO);

    static final ParametrosCredito.PesosScore PESOS = new ParametrosCredito.PesosScore(
            bd(250), bd(200), bd(200), bd(150), bd(100), bd(100), 15, 180, bd(50), bd(5), bd(100), bd(70), bd(40));

    static ParametrosCredito parametros(ParametrosCredito.PesosScore pesos) {
        return new ParametrosCredito(30, 3, 20, bd(30), Dinero.de("20000"), Dinero.de("3300"), new BigDecimal("114.68"),
                Dinero.de("20"), 4, Map.of(
                        1, new CondicionesNivel(1, bd(25), 8, bd(95), 1),
                        2, new CondicionesNivel(2, bd(50), 12, bd(90), 1),
                        3, new CondicionesNivel(3, bd(75), 16, bd(80), 2),
                        4, new CondicionesNivel(4, bd(100), 24, bd(70), 0)),
                650, 500, bd(60), pesos, 5, 15, 10, 2, 8, 9, 31, 12);
    }

    public static final ParametrosCredito P = parametros(PESOS);

    static BigDecimal bd(int v) { return BigDecimal.valueOf(v); }

    /** 12 semanas con cobros, 60 cobros de 15 pagadores, 200 días de antigüedad. */
    static CobrosDelPeriodo cobrosSemanales(String porSemana) {
        return new CobrosDelPeriodo(12, Dinero.de(porSemana).multiplicar(bd(12)), 12, 60, 15, 200);
    }

    static Evaluacion ok(Result<Evaluacion, ErrorEvaluacion> r) {
        assertThat(r).isInstanceOf(Result.Ok.class);
        return ((Result.Ok<Evaluacion, ErrorEvaluacion>) r).valor();
    }

    static ErrorEvaluacion err(Result<Evaluacion, ErrorEvaluacion> r) {
        assertThat(r).isInstanceOf(Result.Err.class);
        return ((Result.Err<Evaluacion, ErrorEvaluacion>) r).error();
    }

    static Result<Evaluacion, ErrorEvaluacion> evaluar(String monto, int cuotas, CobrosDelPeriodo cobros,
                                                       InformeCentral central, HistorialCrediticio h, ParametrosCredito p) {
        return PoliticaCredito.evaluar(CLIENTE, new SolicitudCredito(Dinero.de(monto), cuotas, Destino.CAPITAL_TRABAJO),
                cobros, central, h, p, AHORA);
    }

    // ------------------------------------------------------------------ ejemplos del SRS

    /** RN-13: Ana, nivel 1, cobra S/ 1 500 por semana: capacidad de unos S/ 3 400 y recibe unos S/ 850. */
    @Test
    void ana() {
        var e = ok(evaluar("3000", 8, cobrosSemanales("1500"), SIN_HISTORIAL, HistorialCrediticio.nuevo(CLIENTE), P));

        assertThat(e.recomendacion()).isEqualTo(Recomendacion.APTO);
        assertThat(e.condiciones().monto().valor()).isCloseTo(new BigDecimal("850"), within(new BigDecimal("2")));
        assertThat(e.condiciones().tea()).isEqualByComparingTo("95");
        assertThat(e.condiciones().cuotas()).isEqualTo(8);
        assertThat(e.condiciones().porcentajeRetencion()).isEqualTo(8);   // 112.5 / 1500 = 7.5 % → 8
        assertThat(e.requiereAval()).isTrue();                               // nivel 1 (RN-21)
        // regularidad 250 + holgura 23,4 (cuota de S/ 397 de S/ 450) + historial nuevo 100 + pagadores 150
        // + antigüedad 100 + sin historial SBS 70 = 693,4
        assertThat(e.score()).isEqualTo(693);
        assertThat(e.factores()).hasSize(6);
    }

    /** RN-13: Pedro, nivel 3, cobra S/ 300 por semana: capacidad de unos S/ 1 310 (16 cuotas, 80 %) y recibe unos S/ 980. */
    @Test
    void pedro() {
        var pedro = HistorialCrediticio.reconstituir(CLIENTE, 3, 0, 2, false, 0L);
        var e = ok(evaluar("1500", 16, cobrosSemanales("300"), new InformeCentral(ClasificacionSbs.NORMAL, Dinero.CERO),
                pedro, P));

        assertThat(e.recomendacion()).isEqualTo(Recomendacion.APTO);
        assertThat(e.condiciones().monto().valor()).isCloseTo(new BigDecimal("982"), within(new BigDecimal("3")));
        assertThat(e.condiciones().tea()).isEqualByComparingTo("80");
        assertThat(e.requiereAval()).isFalse();                              // nivel 3: sin aval
    }

    @Test
    void elMontoNuncaSuperaLoPedidoNiElTopePorOperacion() {
        var nivel4 = HistorialCrediticio.reconstituir(CLIENTE, 4, 0, 0, false, 0L);
        var poco = ok(evaluar("500", 8, cobrosSemanales("1500"), SIN_HISTORIAL, nivel4, P));
        assertThat(poco.condiciones().monto()).isEqualTo(Dinero.de("500"));

        var mucho = ok(evaluar("9000", 24, cobrosSemanales("5000"), SIN_HISTORIAL, nivel4, P));
        assertThat(mucho.condiciones().monto()).isEqualTo(Dinero.de("3300"));   // RN-01
    }

    /** Con menos cuotas que el máximo del nivel, la cuota tampoco puede pasar del 30 % de los cobros (RN-12). */
    @Test
    void laCuotaNoSuperaElTreintaPorCientoDeLosCobros() {
        var nivel4 = HistorialCrediticio.reconstituir(CLIENTE, 4, 0, 0, false, 0L);
        var e = ok(evaluar("3000", 4, cobrosSemanales("1000"), SIN_HISTORIAL, nivel4, P));
        assertThat(e.condiciones().cuota().valor()).isLessThanOrEqualTo(new BigDecimal("300"));
    }

    @Test
    void laTeaNuncaSuperaLaTasaMaxima() {
        var bajaTasa = new ParametrosCredito(30, 3, 20, bd(30), Dinero.de("20000"), Dinero.de("3300"), bd(85),
                Dinero.de("20"), 4, P.niveles(), 650, 500, bd(60), PESOS, 5, 15, 10, 2, 8, 9, 31, 12);
        var e = ok(evaluar("800", 8, cobrosSemanales("1500"), SIN_HISTORIAL, HistorialCrediticio.nuevo(CLIENTE), bajaTasa));
        assertThat(e.condiciones().tea()).isEqualByComparingTo("85");         // RN-16: min(95, 85)
    }

    // ------------------------------------------------------------------ elegibilidad y eliminatorias

    @Test
    void sinUsoMinimoNoSeEvaluaYSeDiceQueFalta() {
        var nueva = new CobrosDelPeriodo(2, Dinero.de("600"), 2, 8, 3, 20);
        var e = err(evaluar("500", 8, nueva, SIN_HISTORIAL, HistorialCrediticio.nuevo(CLIENTE), P));
        assertThat(e).isInstanceOf(ErrorEvaluacion.BilleteraSinUsoMinimo.class);
        assertThat(e.mensaje()).contains("30 días").contains("3 semanas").contains("20 cobros");
    }

    @Test
    void lasCuotasDebenEstarEnElRangoDelNivel() {
        assertThat(err(evaluar("500", 9, cobrosSemanales("1500"), SIN_HISTORIAL, HistorialCrediticio.nuevo(CLIENTE), P)))
                .isEqualTo(new ErrorEvaluacion.CuotasFueraDeRango(4, 8));
        assertThat(err(evaluar("500", 3, cobrosSemanales("1500"), SIN_HISTORIAL, HistorialCrediticio.nuevo(CLIENTE), P)))
                .isEqualTo(new ErrorEvaluacion.CuotasFueraDeRango(4, 8));
    }

    @Test
    void clasificacionDeficienteEsNoAptoSinScore() {
        var e = ok(evaluar("500", 8, cobrosSemanales("1500"), new InformeCentral(ClasificacionSbs.DEFICIENTE, Dinero.de("3000")),
                HistorialCrediticio.nuevo(CLIENTE), P));
        assertThat(e.recomendacion()).isEqualTo(Recomendacion.NO_APTO);
        assertThat(e.score()).isNull();
        assertThat(e.motivo()).contains("DEFICIENTE");
        assertThat(e.condiciones()).isNull();
        assertThat(e.requiereAval()).isFalse();
    }

    @Test
    void deudaTotalMasElCreditoMayorQueVeinteMilEsNoApto() {
        var central = new InformeCentral(ClasificacionSbs.NORMAL, Dinero.de("19500"));
        assertThat(ok(evaluar("500", 8, cobrosSemanales("1500"), central, HistorialCrediticio.nuevo(CLIENTE), P)).recomendacion())
                .isEqualTo(Recomendacion.APTO);                               // 20 000 justo: pasa
        assertThat(ok(evaluar("500.01", 8, cobrosSemanales("1500"), central, HistorialCrediticio.nuevo(CLIENTE), P)).recomendacion())
                .isEqualTo(Recomendacion.NO_APTO);
    }

    @Test
    void conBloqueoInternoEsNoApto() {
        var bloqueado = HistorialCrediticio.reconstituir(CLIENTE, 1, 0, 35, true, 0L);
        var e = ok(evaluar("500", 8, cobrosSemanales("1500"), SIN_HISTORIAL, bloqueado, P));
        assertThat(e.recomendacion()).isEqualTo(Recomendacion.NO_APTO);
        assertThat(e.motivo()).contains("bloqueo interno");
    }

    @Test
    void sinCapacidadParaLaCuotaMinimaEsNoApto() {
        var pocos = new CobrosDelPeriodo(12, Dinero.de("480"), 12, 60, 15, 200);   // S/ 40 por semana
        var e = ok(evaluar("500", 8, pocos, SIN_HISTORIAL, HistorialCrediticio.nuevo(CLIENTE), P));
        assertThat(e.recomendacion()).isEqualTo(Recomendacion.NO_APTO);
        assertThat(e.motivo()).contains("cuota mínima");
        assertThat(e.score()).isNotNull();
    }

    // ------------------------------------------------------------------ score y recomendación

    /** Con todo el peso en la antigüedad y referencia de 1000 días, el score es igual a los días: se prueban los bordes. */
    @ParameterizedTest
    @CsvSource({"499, NO_APTO", "500, APTO_CON_AJUSTE", "649, APTO_CON_AJUSTE", "650, APTO"})
    void bordesDelScore(int dias, Recomendacion esperada) {
        var soloAntiguedad = new ParametrosCredito.PesosScore(bd(0), bd(0), bd(0), bd(0), bd(1000), bd(0),
                15, 1000, bd(50), bd(5), bd(100), bd(70), bd(40));
        var cobros = new CobrosDelPeriodo(12, Dinero.de("18000"), 12, 60, 15, dias);
        var e = ok(evaluar("1000", 8, cobros, SIN_HISTORIAL, HistorialCrediticio.nuevo(CLIENTE), parametros(soloAntiguedad)));
        assertThat(e.score()).isEqualTo(dias);
        assertThat(e.recomendacion()).isEqualTo(esperada);
    }

    @Test
    void elAjusteOfreceComoMaximoElSesentaPorCientoDeLoPedido() {
        var soloAntiguedad = new ParametrosCredito.PesosScore(bd(0), bd(0), bd(0), bd(0), bd(1000), bd(0),
                15, 1000, bd(50), bd(5), bd(100), bd(70), bd(40));
        var cobros = new CobrosDelPeriodo(12, Dinero.de("18000"), 12, 60, 15, 600);
        var nivel4 = HistorialCrediticio.reconstituir(CLIENTE, 4, 0, 0, false, 0L);
        var e = ok(evaluar("1000", 8, cobros, SIN_HISTORIAL, nivel4, parametros(soloAntiguedad)));
        assertThat(e.recomendacion()).isEqualTo(Recomendacion.APTO_CON_AJUSTE);
        assertThat(e.condiciones().monto()).isEqualTo(Dinero.de("600"));
        assertThat(e.motivo()).contains("60 %");
    }

    @Test
    void unClienteEnCppRecibeComoMaximoUnAjuste() {
        var e = ok(evaluar("1000", 8, cobrosSemanales("1500"), new InformeCentral(ClasificacionSbs.CPP, Dinero.de("500")),
                HistorialCrediticio.nuevo(CLIENTE), P));
        assertThat(e.score()).isGreaterThanOrEqualTo(650);
        assertThat(e.recomendacion()).isEqualTo(Recomendacion.APTO_CON_AJUSTE);
        assertThat(e.condiciones().monto().valor()).isLessThanOrEqualTo(new BigDecimal("600"));
        assertThat(e.motivo()).contains("CPP");
    }

    @Test
    void elHistorialEnKapikuaPesaEnElScore() {
        var sinCreditos = ok(evaluar("800", 8, cobrosSemanales("1500"), SIN_HISTORIAL, HistorialCrediticio.nuevo(CLIENTE), P));
        var pagoATiempo = ok(evaluar("800", 8, cobrosSemanales("1500"), SIN_HISTORIAL,
                HistorialCrediticio.reconstituir(CLIENTE, 1, 0, 2, false, 0L), P));
        // Nuevo: 50 × 2 = 100 puntos. Con 2 días de atraso: (100 − 10) × 2 = 180 puntos.
        assertThat(pagoATiempo.score() - sinCreditos.score()).isEqualTo(80);
    }

    // ------------------------------------------------------------------ aval (RN-21)

    @Test
    void elAvalNecesitaIdentidadValidadaClasificacionNormalYNoEstarBloqueado() {
        var normal = new InformeCentral(ClasificacionSbs.NORMAL, Dinero.CERO);
        var limpio = HistorialCrediticio.nuevo(CLIENTE);
        assertThat(PoliticaCredito.evaluarAval(true, normal, limpio)).isInstanceOf(Result.Ok.class);
        assertThat(PoliticaCredito.evaluarAval(false, normal, limpio)).isInstanceOf(Result.Err.class);
        assertThat(PoliticaCredito.evaluarAval(true, SIN_HISTORIAL, limpio)).isInstanceOf(Result.Err.class);
        assertThat(PoliticaCredito.evaluarAval(true, new InformeCentral(ClasificacionSbs.CPP, Dinero.CERO), limpio))
                .isInstanceOf(Result.Err.class);
        assertThat(PoliticaCredito.evaluarAval(true, normal, HistorialCrediticio.reconstituir(CLIENTE, 1, 0, 40, true, 0L)))
                .isInstanceOf(Result.Err.class);
    }
}
