package com.projects.kapikua_api.evaluacion;

import com.projects.kapikua_api.evaluacion.application.port.in.ConsultarEvaluacionUseCase;
import com.projects.kapikua_api.evaluacion.application.port.in.ConsultarEvaluacionUseCase.NivelCliente;
import com.projects.kapikua_api.evaluacion.application.port.in.EvaluarSolicitudUseCase;
import com.projects.kapikua_api.evaluacion.application.port.in.ResultadoEvaluacion;
import com.projects.kapikua_api.evaluacion.domain.error.ErrorEvaluacion;
import com.projects.kapikua_api.evaluacion.domain.model.Destino;
import com.projects.kapikua_api.evaluacion.domain.model.Recomendacion;
import com.projects.kapikua_api.evaluacion.domain.model.SolicitudCredito;
import com.projects.kapikua_api.identidad.application.command.RegistrarUsuarioCommand;
import com.projects.kapikua_api.identidad.application.port.in.GestionarConsentimientosUseCase;
import com.projects.kapikua_api.identidad.application.port.in.RegistrarUsuarioUseCase;
import com.projects.kapikua_api.identidad.domain.error.RegistroError;
import com.projects.kapikua_api.identidad.domain.model.TipoConsentimiento;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.evento.EventosCredito;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Evaluación contra PostgreSQL con el esquema v6 y el cambio 002. Solo corre con KAPIKUA_DB_PRUEBAS
 * (ver BilleteraIntegracionTest). Inserta cobros pasados directamente en la base de prueba.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "KAPIKUA_DB_PRUEBAS", matches = "jdbc:postgresql:.+")
class EvaluacionIntegracionTest {

    @DynamicPropertySource
    static void base(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", () -> System.getenv("KAPIKUA_DB_PRUEBAS"));
    }

    @Autowired RegistrarUsuarioUseCase registrar;
    @Autowired GestionarConsentimientosUseCase consentimientos;
    @Autowired EvaluarSolicitudUseCase evaluar;
    @Autowired ConsultarEvaluacionUseCase consultar;
    @Autowired ApplicationEventPublisher eventos;
    @Autowired TransactionTemplate transaccion;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;

    // ------------------------------------------------------------------ preparación

    /** Registra un cliente cuyo DNI termina en {@code digito} (la Central simulada responde según ese dígito). */
    private UsuarioId cliente(char digito, boolean conConsentimiento) {
        var r = ThreadLocalRandom.current();
        var dni = String.valueOf(1_000_000 + r.nextInt(8_999_999)) + digito;
        var celular = "9" + (10_000_000 + r.nextInt(89_999_999));
        var resultado = registrar.registrar(new RegistrarUsuarioCommand(dni, LocalDate.of(1990, 1, 1),
                "e" + dni + "@prueba.pe", celular, "Negocio " + dni, "Kapikua2026", true));
        assertThat(resultado).isInstanceOf(Result.Ok.class);
        var id = ((Result.Ok<UsuarioId, RegistroError>) resultado).valor();
        if (conConsentimiento) consentimientos.otorgar(id, TipoConsentimiento.EVALUACION_CON_BILLETERA);
        return id;
    }

    /**
     * Abre la billetera hace 200 días y le agrega, en cada una de las últimas 12 semanas completas,
     * 5 cobros de S/ 300 (S/ 1 500 por semana) de 15 pagadores distintos: 60 cobros en total.
     */
    private void conHistoriaDeCobros(UsuarioId cliente) {
        var billetera = jdbc.queryForObject("select id from billetera.billetera where usuario_id = ?", Long.class, cliente.valor());
        jdbc.update("update billetera.billetera set fecha_apertura = now() - interval '200 days' where id = ?", billetera);
        var pagadores = new ArrayList<Long>();
        for (int i = 0; i < 15; i++) {
            var r = ThreadLocalRandom.current();
            pagadores.add(jdbc.queryForObject("""
                    insert into billetera.billetera (usuario_id, numero_celular, saldo, fecha_apertura)
                    values (?, ?, 5000, now() - interval '300 days') returning id""", Long.class,
                    5_000_000L + r.nextInt(4_000_000), "9" + (10_000_000 + r.nextInt(89_999_999))));
        }
        var lunesActual = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for (int semana = 1; semana <= 12; semana++) {
            for (int k = 0; k < 5; k++) {
                var fecha = Timestamp.from(lunesActual.minusWeeks(semana).plusDays(k)
                        .atTime(10, 0).atZone(clock.getZone()).toInstant());
                var tx = jdbc.queryForObject("""
                        insert into billetera.transaccion (tipo, monto, referencia, fecha)
                        values ('PAGO_NEGOCIO', 300, ?, ?) returning id""", Long.class, UUID.randomUUID().toString(), fecha);
                jdbc.update("insert into billetera.movimiento (transaccion_id, billetera_id, monto, saldo_posterior, fecha) values (?, ?, -300, 1000, ?)",
                        tx, pagadores.get(((semana - 1) * 5 + k) % 15), fecha);
                jdbc.update("insert into billetera.movimiento (transaccion_id, billetera_id, monto, saldo_posterior, fecha) values (?, ?, 300, 1000, ?)",
                        tx, billetera, fecha);
            }
        }
    }

    private static SolicitudCredito solicitud(String monto, int cuotas) {
        return new SolicitudCredito(Dinero.de(monto), cuotas, Destino.CAPITAL_TRABAJO);
    }

    private static ResultadoEvaluacion ok(Result<ResultadoEvaluacion, ErrorEvaluacion> r) {
        assertThat(r).isInstanceOf(Result.Ok.class);
        return ((Result.Ok<ResultadoEvaluacion, ErrorEvaluacion>) r).valor();
    }

    private static ErrorEvaluacion err(Result<?, ErrorEvaluacion> r) {
        assertThat(r).isInstanceOf(Result.Err.class);
        return ((Result.Err<?, ErrorEvaluacion>) r).error();
    }

    private int evaluacionesDe(UsuarioId u) {
        return jdbc.queryForObject("select count(*) from evaluacion.evaluacion where usuario_id = ?", Integer.class, u.valor());
    }

    // ------------------------------------------------------------------ pruebas

    @Test
    void sinConsentimientoNoSeEvalua() {
        var u = cliente('0', false);
        conHistoriaDeCobros(u);
        assertThat(err(evaluar.evaluar(u, solicitud("1000", 8)))).isInstanceOf(ErrorEvaluacion.ConsentimientoFaltante.class);
        assertThat(evaluacionesDe(u)).isZero();
    }

    @Test
    void unaBilleteraNuevaNoCumpleElUsoMinimoYNoSeGuardaEvaluacion() {
        var u = cliente('0', true);
        var e = err(evaluar.evaluar(u, solicitud("1000", 8)));
        assertThat(e).isInstanceOf(ErrorEvaluacion.BilleteraSinUsoMinimo.class);
        assertThat(evaluacionesDe(u)).isZero();
    }

    /** Ana: S/ 1 500 por semana, nivel 1, sin historial en la Central → APTO por unos S/ 850, con aval (RN-13, RN-21). */
    @Test
    void anaRecibeUnaRecomendacionAptaQueQuedaGuardada() {
        var u = cliente('0', true);
        conHistoriaDeCobros(u);
        var inicio = System.nanoTime();
        var r = ok(evaluar.evaluar(u, solicitud("3000", 8)));
        var milisegundos = (System.nanoTime() - inicio) / 1_000_000;

        assertThat(milisegundos).isLessThan(3000);                         // RNF-14
        assertThat(r.recomendacion()).isEqualTo(Recomendacion.APTO);
        assertThat(r.condiciones().monto().valor()).isCloseTo(new BigDecimal("850"), within(new BigDecimal("3")));
        assertThat(r.condiciones().tea()).isEqualByComparingTo("95");
        assertThat(r.requiereAval()).isTrue();

        var guardada = consultar.porId(r.evaluacionId()).orElseThrow();
        assertThat(guardada).usingRecursiveComparison().ignoringFields("fecha").isEqualTo(r);
        assertThat(guardada.fecha().toInstant()).isEqualTo(r.fecha().toInstant());   // mismo instante, otra zona
        assertThat(jdbc.queryForObject("select jsonb_array_length(factores) from evaluacion.evaluacion where id = ?",
                Integer.class, r.evaluacionId())).isEqualTo(6);
    }

    @Test
    void clasificacionDeficienteQuedaComoNoAptoSinScore() {
        var u = cliente('8', true);
        conHistoriaDeCobros(u);
        var r = ok(evaluar.evaluar(u, solicitud("1000", 8)));
        assertThat(r.recomendacion()).isEqualTo(Recomendacion.NO_APTO);
        assertThat(r.score()).isNull();
        assertThat(r.motivo()).contains("DEFICIENTE");
        assertThat(evaluacionesDe(u)).isEqualTo(1);
    }

    @Test
    void clasificacionCppRecibeUnAjuste() {
        var u = cliente('7', true);
        conHistoriaDeCobros(u);
        var r = ok(evaluar.evaluar(u, solicitud("1000", 8)));
        assertThat(r.recomendacion()).isEqualTo(Recomendacion.APTO_CON_AJUSTE);
        assertThat(r.condiciones().monto().valor()).isLessThanOrEqualTo(new BigDecimal("600"));
    }

    @Test
    void elAvalNecesitaClasificacionNormal() {
        assertThat(evaluar.evaluarAval(cliente('5', false))).isInstanceOf(Result.Ok.class);
        assertThat(err(evaluar.evaluarAval(cliente('0', false)))).isInstanceOf(ErrorEvaluacion.AvalClasificacionNoNormal.class);
        assertThat(err(evaluar.evaluarAval(cliente('7', false)))).isInstanceOf(ErrorEvaluacion.AvalClasificacionNoNormal.class);
    }

    /** Los eventos de Créditos mueven la escalera: un crédito pagado a tiempo sube del nivel 1 al 2. */
    @Test
    void unCreditoPagadoATiempoSubeDeNivelYCambiaLasCondiciones() {
        var u = cliente('0', true);
        conHistoriaDeCobros(u);
        transaccion.executeWithoutResult(s -> {
            eventos.publishEvent(new EventosCredito.PrestamoDesembolsado(u, "EFA-C-1"));
            eventos.publishEvent(new EventosCredito.AtrasoDetectado(u, 1L, 3));
            eventos.publishEvent(new EventosCredito.PrestamoPagado(u, 1L, false));
        });

        var nivel = ((Result.Ok<NivelCliente, ErrorEvaluacion>) consultar.nivel(u)).valor();
        assertThat(nivel.nivel()).isEqualTo(2);
        assertThat(nivel.maxCuotas()).isEqualTo(12);
        assertThat(nivel.usoMinimo().cumple()).isTrue();
        assertThat(nivel.usoMinimo().promedioSemanal()).isEqualTo(Dinero.de("1500"));

        var r = ok(evaluar.evaluar(u, solicitud("3000", 12)));
        assertThat(r.condiciones().tea()).isEqualByComparingTo("90");     // TEA del nivel 2
        assertThat(r.condiciones().monto().valor()).isGreaterThan(new BigDecimal("2000"));  // 50 % de su capacidad
    }

    @Test
    void siPagaElAvalElClienteQuedaBloqueadoYEsNoApto() {
        var u = cliente('0', true);
        conHistoriaDeCobros(u);
        transaccion.executeWithoutResult(s -> {
            eventos.publishEvent(new EventosCredito.PrestamoDesembolsado(u, "EFA-C-2"));
            eventos.publishEvent(new EventosCredito.AtrasoDetectado(u, 2L, 16));
            eventos.publishEvent(new EventosCredito.PrestamoPagado(u, 2L, true));
        });
        var r = ok(evaluar.evaluar(u, solicitud("500", 8)));
        assertThat(r.recomendacion()).isEqualTo(Recomendacion.NO_APTO);
        assertThat(r.motivo()).contains("bloqueo interno");
        assertThat(err(evaluar.evaluarAval(u))).isInstanceOf(ErrorEvaluacion.AvalClasificacionNoNormal.class);
    }

    @Test
    void lasCuotasDependenDelNivel() {
        var u = cliente('0', true);
        conHistoriaDeCobros(u);
        assertThat(err(evaluar.evaluar(u, solicitud("1000", 12))))
                .isEqualTo(new ErrorEvaluacion.CuotasFueraDeRango(4, 8));
    }
}
