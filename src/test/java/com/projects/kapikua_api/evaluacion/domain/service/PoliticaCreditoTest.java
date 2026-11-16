package com.projects.kapikua_api.evaluacion.domain.service;

import com.projects.kapikua_api.evaluacion.domain.model.*;

import com.projects.kapikua_api.evaluacion.domain.error.ErrorEvaluacion;
import com.projects.kapikua_api.evaluacion.domain.model.ConfiguracionFactoresScore.PonderacionesScore;
import com.projects.kapikua_api.evaluacion.domain.model.ConfiguracionFactoresScore.PuntajesClasificacionSbs;
import com.projects.kapikua_api.evaluacion.domain.model.ConfiguracionFactoresScore.ReferenciasScore;
import com.projects.kapikua_api.evaluacion.domain.model.ReglasScore.ReglasRecomendacion;
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

public class PoliticaCreditoTest {
    public static final UsuarioId CLIENTE = new UsuarioId(1L);
    static final OffsetDateTime AHORA = OffsetDateTime.of(2026, 11, 9, 10, 0, 0, 0, ZoneOffset.ofHours(-5));
    static final InformeCentral SIN_HISTORIAL = new InformeCentral(null, Dinero.CERO);

    static final ConfiguracionFactoresScore FACTORES = new ConfiguracionFactoresScore(
            new PonderacionesScore(bd(250), bd(200), bd(200), bd(150), bd(100), bd(100)),
            new ReferenciasScore(15, 180, bd(50), bd(5)), new PuntajesClasificacionSbs(bd(100), bd(70), bd(40)));

    static ParametrosCredito parametros(ConfiguracionFactoresScore factores) {
        return new ParametrosCredito(new UsoMinimo(30, 3, 20, 12),
                new LimitesCredito(bd(30), Dinero.de("20000"), Dinero.de("3300"), new BigDecimal("114.68"),
                        Dinero.de("20"), 4),
                new ReglasScore(new ReglasRecomendacion(650, 500, bd(60)), factores),
                new ReglasRetencion(5, 15, 10), new ReglasNivel(Map.of(
                        1, new CondicionesNivel(1, bd(25), 8, bd(95), 1),
                        2, new CondicionesNivel(2, bd(50), 12, bd(90), 1),
                        3, new CondicionesNivel(3, bd(75), 16, bd(80), 2),
                        4, new CondicionesNivel(4, bd(100), 24, bd(70), 0)), 2, 8, 9, 31));
    }

    public static final ParametrosCredito P = parametros(FACTORES);

    private static ParametrosCredito politicaSoloAntiguedad() {
        return parametros(new ConfiguracionFactoresScore(
                new PonderacionesScore(bd(0), bd(0), bd(0), bd(0), bd(1000), bd(0)),
                new ReferenciasScore(15, 1000, bd(50), bd(5)), FACTORES.puntajesSbs()));
    }

    static BigDecimal bd(int v) { return BigDecimal.valueOf(v); }

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

    @Test
    void anaRecibeUnAjusteAlMontoViable() {
        var e = ok(evaluar("3000", 8, cobrosSemanales("1500"), SIN_HISTORIAL, HistorialCrediticio.nuevo(CLIENTE), P));

        assertThat(e.recomendacion()).isEqualTo("APTO_CON_AJUSTE");
        assertThat(e.esApta()).isTrue();
        assertThat(e.condiciones().monto().valor()).isCloseTo(new BigDecimal("850"), within(new BigDecimal("2")));
        assertThat(e.condiciones().tea()).isEqualByComparingTo("95");
        assertThat(e.condiciones().cuotas()).isEqualTo(8);
        assertThat(e.condiciones().porcentajeRetencion()).isEqualTo(8);
        assertThat(e.requiereAval()).isTrue();
        assertThat(e.score()).isEqualTo(693);
        assertThat(e.factores()).hasSize(6);
    }

    @Test
    void pedroRecibeAjusteSinRequerirAval() {
        var pedro = HistorialCrediticio.reconstituir(CLIENTE, 3, 0, 2, false, 0L);
        var e = ok(evaluar("1500", 16, cobrosSemanales("300"), new InformeCentral(ClasificacionSbs.NORMAL, Dinero.CERO),
                pedro, P));

        assertThat(e.recomendacion()).isEqualTo("APTO_CON_AJUSTE");
        assertThat(e.condiciones().monto().valor()).isCloseTo(new BigDecimal("982"), within(new BigDecimal("3")));
        assertThat(e.condiciones().tea()).isEqualByComparingTo("80");
        assertThat(e.requiereAval()).isFalse();
    }

    @Test
    void elMontoNuncaSuperaLoPedidoNiElTopePorOperacion() {
        var nivel4 = HistorialCrediticio.reconstituir(CLIENTE, 4, 0, 0, false, 0L);
        var poco = ok(evaluar("500", 8, cobrosSemanales("1500"), SIN_HISTORIAL, nivel4, P));
        assertThat(poco.condiciones().monto()).isEqualTo(Dinero.de("500"));

        var mucho = ok(evaluar("9000", 24, cobrosSemanales("5000"), SIN_HISTORIAL, nivel4, P));
        assertThat(mucho.condiciones().monto()).isEqualTo(Dinero.de("3300"));
    }

    @Test
    void laCuotaNoSuperaElTreintaPorCientoDeLosCobros() {
        var nivel4 = HistorialCrediticio.reconstituir(CLIENTE, 4, 0, 0, false, 0L);
        var e = ok(evaluar("3000", 4, cobrosSemanales("1000"), SIN_HISTORIAL, nivel4, P));
        assertThat(e.condiciones().cuota().valor()).isLessThanOrEqualTo(new BigDecimal("300"));
    }

    @Test
    void laTeaNuncaSuperaLaTasaMaxima() {
        var limites = P.limitesCredito();
        var bajaTasa = new ParametrosCredito(P.usoMinimo(), new LimitesCredito(limites.porcentajeCapacidadPago(),
                limites.deudaMaxima(), limites.topePorOperacion(), bd(85), limites.cuotaMinima(), limites.cuotasMinimas()),
                P.score(), P.retencion(), P.reglasNivel());
        var e = ok(evaluar("800", 8, cobrosSemanales("1500"), SIN_HISTORIAL, HistorialCrediticio.nuevo(CLIENTE), bajaTasa));
        assertThat(e.condiciones().tea()).isEqualByComparingTo("85");
    }

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
        assertThat(e.recomendacion()).isEqualTo("NO_APTO");
        assertThat(e.score()).isNull();
        assertThat(e.motivo()).contains("DEFICIENTE");
        assertThat(e.condiciones().hayOferta()).isFalse();
        assertThat(e.condiciones()).isEqualTo(CondicionesSugeridas.ninguna());
        assertThat(e.esApta()).isFalse();
        assertThat(e.requiereAval()).isFalse();
    }

    @Test
    void deudaTotalMasElCreditoMayorQueVeinteMilEsNoApto() {
        var central = new InformeCentral(ClasificacionSbs.NORMAL, Dinero.de("19500"));
        assertThat(ok(evaluar("500", 8, cobrosSemanales("1500"), central, HistorialCrediticio.nuevo(CLIENTE), P)).recomendacion())
                .isEqualTo("APTO");
        assertThat(ok(evaluar("500.01", 8, cobrosSemanales("1500"), central, HistorialCrediticio.nuevo(CLIENTE), P)).recomendacion())
                .isEqualTo("NO_APTO");
    }

    @Test
    void conBloqueoInternoEsNoApto() {
        var bloqueado = HistorialCrediticio.reconstituir(CLIENTE, 1, 0, 35, true, 0L);
        var e = ok(evaluar("500", 8, cobrosSemanales("1500"), SIN_HISTORIAL, bloqueado, P));
        assertThat(e.recomendacion()).isEqualTo("NO_APTO");
        assertThat(e.motivo()).contains("bloqueo interno");
    }

    @Test
    void sinCapacidadParaLaCuotaMinimaEsNoApto() {
        var pocos = new CobrosDelPeriodo(12, Dinero.de("480"), 12, 60, 15, 200);
        var e = ok(evaluar("500", 8, pocos, SIN_HISTORIAL, HistorialCrediticio.nuevo(CLIENTE), P));
        assertThat(e.recomendacion()).isEqualTo("NO_APTO");
        assertThat(e.motivo()).contains("cuota mínima");
        assertThat(e.score()).isNotNull();
    }

    @ParameterizedTest
    @CsvSource({"499, NO_APTO", "500, APTO_CON_AJUSTE", "649, APTO_CON_AJUSTE", "650, APTO"})
    void bordesDelScore(int dias, String esperada) {
        var cobros = new CobrosDelPeriodo(12, Dinero.de("18000"), 12, 60, 15, dias);
        var e = ok(evaluar("800", 8, cobros, SIN_HISTORIAL, HistorialCrediticio.nuevo(CLIENTE), politicaSoloAntiguedad()));
        assertThat(e.score()).isEqualTo(dias);
        assertThat(e.recomendacion()).isEqualTo(esperada);
    }

    @Test
    void elAjusteOfreceComoMaximoElSesentaPorCientoDeLoPedido() {
        var cobros = new CobrosDelPeriodo(12, Dinero.de("18000"), 12, 60, 15, 600);
        var nivel4 = HistorialCrediticio.reconstituir(CLIENTE, 4, 0, 0, false, 0L);
        var e = ok(evaluar("1000", 8, cobros, SIN_HISTORIAL, nivel4, politicaSoloAntiguedad()));
        assertThat(e.recomendacion()).isEqualTo("APTO_CON_AJUSTE");
        assertThat(e.condiciones().monto()).isEqualTo(Dinero.de("600"));
        assertThat(e.motivo()).contains("60 %");
    }

    @Test
    void unClienteEnCppRecibeComoMaximoUnAjuste() {
        var e = ok(evaluar("1000", 8, cobrosSemanales("1500"), new InformeCentral(ClasificacionSbs.CPP, Dinero.de("500")),
                HistorialCrediticio.nuevo(CLIENTE), P));
        assertThat(e.score()).isGreaterThanOrEqualTo(650);
        assertThat(e.recomendacion()).isEqualTo("APTO_CON_AJUSTE");
        assertThat(e.condiciones().monto().valor()).isLessThanOrEqualTo(new BigDecimal("600"));
        assertThat(e.motivo()).contains("CPP");
    }

    @Test
    void elScoreBajoPrevaleceSobreElAjustePorCpp() {
        var cobros = new CobrosDelPeriodo(12, Dinero.de("18000"), 12, 60, 15, 499);
        var cpp = new InformeCentral(ClasificacionSbs.CPP, Dinero.CERO);
        var e = ok(evaluar("1000", 8, cobros, cpp, HistorialCrediticio.nuevo(CLIENTE), politicaSoloAntiguedad()));

        assertThat(e.score()).isEqualTo(499);
        assertThat(e.recomendacion()).isEqualTo("NO_APTO");
        assertThat(e.motivo()).contains("Score de 499");
        assertThat(e.condiciones().hayOferta()).isFalse();
    }

    @Test
    void seVerificaLaCuotaMinimaSobreElMontoAjustado() {
        var cobros = new CobrosDelPeriodo(12, Dinero.de("18000"), 12, 60, 15, 650);
        var historial = HistorialCrediticio.nuevo(CLIENTE);
        var politica = politicaSoloAntiguedad();
        var sinAjuste = ok(evaluar("200", 8, cobros, SIN_HISTORIAL, historial, politica));
        var conAjuste = ok(evaluar("200", 8, cobros, new InformeCentral(ClasificacionSbs.CPP, Dinero.CERO),
                historial, politica));

        assertThat(sinAjuste.recomendacion()).isEqualTo("APTO");
        assertThat(conAjuste.recomendacion()).isEqualTo("NO_APTO");
        assertThat(conAjuste.motivo()).contains("cuota mínima");
        assertThat(conAjuste.condiciones().hayOferta()).isFalse();
    }

    @Test
    void elHistorialEnKapikuaPesaEnElScore() {
        var sinCreditos = ok(evaluar("800", 8, cobrosSemanales("1500"), SIN_HISTORIAL, HistorialCrediticio.nuevo(CLIENTE), P));
        var pagoATiempo = ok(evaluar("800", 8, cobrosSemanales("1500"), SIN_HISTORIAL,
                HistorialCrediticio.reconstituir(CLIENTE, 1, 0, 2, false, 0L), P));
        assertThat(pagoATiempo.score() - sinCreditos.score()).isEqualTo(80);
    }

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
