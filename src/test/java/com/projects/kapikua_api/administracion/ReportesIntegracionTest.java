package com.projects.kapikua_api.administracion;

import com.projects.kapikua_api.DatosDePrueba;
import com.projects.kapikua_api.billetera.application.port.in.EnviarDineroUseCase;
import com.projects.kapikua_api.billetera.application.port.in.RecargarSaldoUseCase;
import com.projects.kapikua_api.credito.application.port.in.AceptarOfertaUseCase;
import com.projects.kapikua_api.credito.application.port.in.GestionarAvalUseCase;
import com.projects.kapikua_api.credito.application.port.in.SolicitarCreditoUseCase;
import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.evaluacion.domain.model.Destino;
import com.projects.kapikua_api.identidad.application.port.in.RegistrarUsuarioUseCase;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** CU-16: reportes de solo lectura con DNI enmascarado (RN-30). Solo con KAPIKUA_DB_PRUEBAS. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "KAPIKUA_DB_PRUEBAS", matches = "jdbc:postgresql:.+")
class ReportesIntegracionTest {

    @DynamicPropertySource
    static void base(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", () -> System.getenv("KAPIKUA_DB_PRUEBAS"));
    }

    @Autowired RegistrarUsuarioUseCase registrar;
    @Autowired RecargarSaldoUseCase recargar;
    @Autowired EnviarDineroUseCase enviar;
    @Autowired SolicitarCreditoUseCase solicitar;
    @Autowired GestionarAvalUseCase avales;
    @Autowired AceptarOfertaUseCase aceptar;
    @Autowired ReportesConsulta reportes;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;
    DatosDePrueba datos;
    Periodo mes;

    @BeforeEach
    void preparar() {
        datos = new DatosDePrueba(registrar, jdbc, clock);
        mes = Periodo.de(null, null, LocalDate.now(clock));
    }

    private static <T> T ok(Result<T, ErrorCredito> r) {
        if (r instanceof Result.Err<T, ErrorCredito> e) throw new AssertionError("Se esperaba Ok y fue " + e.error());
        return ((Result.Ok<T, ErrorCredito>) r).valor();
    }

    private static String enmascarado(DatosDePrueba.Cliente c) {
        return "****" + c.dni().substring(4);
    }

    private Long prestamo(DatosDePrueba.Cliente ana) {
        datos.conHistoriaDeCobros(ana.id());
        var luis = datos.cliente('5');
        var r = ok(solicitar.solicitar(ana.id(), Dinero.de("3000"), 8, Destino.CAPITAL_TRABAJO, true));
        ok(avales.designar(ana.id(), r.ofertaId(), luis.celular()));
        var inv = ok(avales.invitacion(luis.id(), r.ofertaId()));
        ok(avales.responder(luis.id(), r.ofertaId(), true, inv.codigoMostrado(), "x"));
        var hoja = ok(aceptar.hojaResumen(ana.id(), r.ofertaId()));
        return ok(aceptar.aceptar(ana.id(), r.ofertaId(), hoja.codigoMostrado(), "x", true));
    }

    @Test
    void elReporteDeClientesEnmascaraElDniYFiltraPorEstadoDeBilletera() {
        var ana = datos.cliente('0');
        var r = reportes.clientes(mes, null);
        assertThat(r.vacio()).isFalse();
        assertThat(r.filas()).allSatisfy(f -> assertThat(f.dni()).matches("\\*{4}\\d{4}"));
        var fila = r.filas().stream().filter(f -> f.dni().equals(enmascarado(ana))).findFirst().orElseThrow();
        assertThat(fila.celular()).contains("***").doesNotContain(ana.celular());
        assertThat(fila.estadoBilletera()).isEqualTo("ACTIVA");
        assertThat(r.indicadores()).extracting(ReportesConsulta.Indicador::etiqueta).contains("Clientes registrados");
        assertThat(r.graficos()).hasSize(2);
        assertThat(r.graficos().getFirst().barras()).hasSize(mes.desde().lengthOfMonth());   // un valor por día del mes

        jdbc.update("update billetera.billetera set estado = 'BLOQUEADA', motivo_bloqueo = 'prueba', version = version + 1 where usuario_id = ?",
                ana.id().valor());
        assertThat(reportes.clientes(mes, "BLOQUEADA").filas()).extracting(ReportesConsulta.FilaCliente::dni).contains(enmascarado(ana));
        assertThat(reportes.clientes(mes, "ACTIVA").filas()).extracting(ReportesConsulta.FilaCliente::dni).doesNotContain(enmascarado(ana));
    }

    @Test
    void elReporteDeTransaccionesMuestraOrigenYDestinoEnmascarados() {
        var ana = datos.cliente('0');
        var luis = datos.cliente('1');
        assertThat(recargar.recargar(ana.id(), Dinero.de("200"), "tok_aprobado", UUID.randomUUID().toString()).esOk()).isTrue();
        assertThat(enviar.pagarNegocio(ana.id(), luis.celular(), Dinero.de("50"), UUID.randomUUID().toString()).esOk()).isTrue();

        var pagos = reportes.transacciones(mes, "PAGO_NEGOCIO");
        assertThat(pagos.filas()).allSatisfy(f -> assertThat(f.tipo()).isEqualTo("PAGO_NEGOCIO"));
        assertThat(pagos.filas()).anySatisfy(f -> {
            assertThat(f.origen()).isEqualTo("DNI " + enmascarado(ana));
            assertThat(f.destino()).isEqualTo("DNI " + enmascarado(luis));
            assertThat(f.monto()).isEqualTo(Dinero.de("50"));
        });
        assertThat(reportes.transacciones(mes, "RECARGA").filas())
                .anySatisfy(f -> assertThat(f.origen()).isEqualTo("Pasarela de pagos"));
    }

    @Test
    void losReportesDeCreditosYCobranzaMuestranElPrestamoYSusCuotas() {
        var ana = datos.cliente('0');
        var id = prestamo(ana);

        var creditos = reportes.creditos(mes, "VIGENTE");
        var fila = creditos.filas().stream().filter(f -> f.id().equals(id)).findFirst().orElseThrow();
        assertThat(fila.dni()).isEqualTo(enmascarado(ana));
        assertThat(fila.cuotas()).isEqualTo(8);
        assertThat(fila.porCobrar().mayorQue(fila.monto())).isTrue();
        assertThat(reportes.creditos(mes, "PAGADO").filas()).extracting(ReportesConsulta.FilaCredito::id).doesNotContain(id);

        var primera = jdbc.queryForObject("select min(fecha_vencimiento) from credito.cuota where prestamo_id = ?", LocalDate.class, id);
        var dia = Periodo.de(primera.toString(), null, LocalDate.now(clock));
        var cobranza = reportes.cobranza(dia, null);
        assertThat(cobranza.filas()).anySatisfy(c -> {
            assertThat(c.prestamoId()).isEqualTo(id);
            assertThat(c.numero()).isEqualTo(1);
            assertThat(c.estado()).isEqualTo("PENDIENTE");
            assertThat(c.dni()).isEqualTo(enmascarado(ana));
        });
        assertThat(reportes.cobranza(dia, "PAGADA").filas()).extracting(ReportesConsulta.FilaCuota::prestamoId).doesNotContain(id);
    }

    @Test
    void sinDatosElReporteQuedaVacioYUnaFechaInvalidaUsaElMesEnCurso() {
        var futuro = Periodo.de(LocalDate.now(clock).plusYears(5).toString(), null, LocalDate.now(clock));
        assertThat(reportes.transacciones(futuro, null).vacio()).isTrue();
        assertThat(reportes.cobranza(futuro, null).vacio()).isTrue();

        var invalido = Periodo.de(null, "2026-13", LocalDate.now(clock));
        assertThat(invalido.aviso()).isNotNull();
        assertThat(invalido.desde()).isEqualTo(mes.desde());
    }
}
