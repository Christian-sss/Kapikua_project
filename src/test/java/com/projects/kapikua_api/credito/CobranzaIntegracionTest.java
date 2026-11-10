package com.projects.kapikua_api.credito;

import com.projects.kapikua_api.DatosDePrueba;
import com.projects.kapikua_api.billetera.application.port.in.EnviarDineroUseCase;
import com.projects.kapikua_api.billetera.application.port.in.RecargarSaldoUseCase;
import com.projects.kapikua_api.credito.application.port.in.AceptarOfertaUseCase;
import com.projects.kapikua_api.credito.application.port.in.CobranzaUseCase;
import com.projects.kapikua_api.credito.application.port.in.ConsultarCreditoUseCase;
import com.projects.kapikua_api.credito.application.port.in.GestionarAvalUseCase;
import com.projects.kapikua_api.credito.application.port.in.PagarPrestamoUseCase;
import com.projects.kapikua_api.credito.application.port.in.SolicitarCreditoUseCase;
import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.credito.domain.model.EstadoAval;
import com.projects.kapikua_api.credito.domain.model.EstadoPrestamo;
import com.projects.kapikua_api.credito.domain.model.ModalidadPago;
import com.projects.kapikua_api.evaluacion.domain.model.Destino;
import com.projects.kapikua_api.identidad.application.port.in.RegistrarUsuarioUseCase;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "KAPIKUA_DB_PRUEBAS", matches = "jdbc:postgresql:.+")
class CobranzaIntegracionTest {
    @DynamicPropertySource
    static void base(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", () -> System.getenv("KAPIKUA_DB_PRUEBAS"));
    }

    @Autowired RegistrarUsuarioUseCase registrar;
    @Autowired SolicitarCreditoUseCase solicitar;
    @Autowired GestionarAvalUseCase avales;
    @Autowired AceptarOfertaUseCase aceptar;
    @Autowired ConsultarCreditoUseCase consultar;
    @Autowired CobranzaUseCase cobranza;
    @Autowired PagarPrestamoUseCase pagos;
    @Autowired RecargarSaldoUseCase recargar;
    @Autowired EnviarDineroUseCase enviar;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;
    DatosDePrueba datos;

    @BeforeEach
    void preparar() {
        datos = new DatosDePrueba(registrar, jdbc, clock);
    }

    private record Escenario(DatosDePrueba.Cliente ana, DatosDePrueba.Cliente luis, Long prestamoId) {}

    private static <T> T ok(Result<T, ErrorCredito> r) {
        if (r instanceof Result.Err<T, ErrorCredito> e) throw new AssertionError("Se esperaba Ok y fue " + e.error());
        return ((Result.Ok<T, ErrorCredito>) r).valor();
    }

    private Escenario prestamoConAval() {
        var ana = datos.cliente('0');
        datos.conHistoriaDeCobros(ana.id());
        var luis = datos.cliente('5');
        var r = ok(solicitar.solicitar(ana.id(), Dinero.de("3000"), 8, Destino.CAPITAL_TRABAJO, true));
        ok(avales.designar(ana.id(), r.ofertaId(), luis.celular()));
        var inv = ok(avales.invitacion(luis.id(), r.ofertaId()));
        ok(avales.responder(luis.id(), r.ofertaId(), true, inv.codigoMostrado(), "x"));
        var hoja = ok(aceptar.hojaResumen(ana.id(), r.ofertaId()));
        return new Escenario(ana, luis, ok(aceptar.aceptar(ana.id(), r.ofertaId(), hoja.codigoMostrado(), "x", true)));
    }

    private void leCompran(DatosDePrueba.Cliente destino, String monto) {
        var comprador = datos.cliente('1');
        var rec = recargar.recargar(comprador.id(), Dinero.de("3000"), "tok_aprobado", UUID.randomUUID().toString());
        assertThat(rec.esOk()).isTrue();
        assertThat(enviar.pagarNegocio(comprador.id(), destino.celular(), Dinero.de(monto), UUID.randomUUID().toString()).esOk()).isTrue();
    }

    private ConsultarCreditoUseCase.DetallePrestamo prestamo(UsuarioId u) {
        return consultar.panel(u).prestamo();
    }

    private LocalDate vencimiento(UsuarioId u, int cuota) {
        return prestamo(u).cuotas().get(cuota - 1).getFechaVencimiento();
    }

    private BigDecimal apartado(UsuarioId u) {
        return jdbc.queryForObject("select saldo_apartado from billetera.billetera where usuario_id = ?", BigDecimal.class, u.valor());
    }

    private int pagos(Long prestamoId, String origen) {
        return jdbc.queryForObject("select count(*) from credito.pago where prestamo_id = ? and origen = ?", Integer.class,
                prestamoId, origen);
    }

    private String estadoAval(Long prestamoId) {
        return jdbc.queryForObject("""
                select a.estado from credito.aval a join credito.prestamo p on p.oferta_id = a.oferta_id
                where p.id = ? and a.estado <> 'RECHAZADO'""", String.class, prestamoId);
    }

    private void sinSaldo(UsuarioId u) {
        jdbc.update("update billetera.billetera set saldo = 0, saldo_apartado = 0, version = version + 1 where usuario_id = ?", u.valor());
        jdbc.update("""
                update credito.cuota set monto_apartado = 0
                where prestamo_id in (select id from credito.prestamo where usuario_id = ?)""", u.valor());
    }

    @Test
    void cadaVentaApartaElPorcentajeParaLaCuota() {
        var e = prestamoConAval();
        leCompran(e.ana(), "500");
        assertThat(apartado(e.ana().id())).isEqualByComparingTo("40.00");
        assertThat(prestamo(e.ana().id()).cuotas().getFirst().getMontoApartado()).isEqualTo(Dinero.de("40"));
        assertThat(jdbc.queryForObject("""
                select count(*) from credito.retencion r join credito.cuota c on c.id = r.cuota_id
                where c.prestamo_id = ? and r.origen = 'DEUDOR'""", Integer.class, e.prestamoId())).isEqualTo(1);
    }

    @Test
    void elCobroDiarioCobraLaCuotaYNoRepite() {
        var e = prestamoConAval();
        leCompran(e.ana(), "500");
        var hoy = vencimiento(e.ana().id(), 1);
        var saldoAntes = datos.saldo(e.ana().id());
        var cuota1 = prestamo(e.ana().id()).cuotas().getFirst().total();

        cobranza.ejecutarCobroDiario(hoy);
        cobranza.ejecutarCobroDiario(hoy);

        var p = prestamo(e.ana().id());
        assertThat(p.cuotas().getFirst().getEstado().name()).isEqualTo("PAGADA");
        assertThat(p.estado()).isEqualTo(EstadoPrestamo.VIGENTE);
        assertThat(pagos(e.prestamoId(), "DEUDOR")).isEqualTo(1);
        assertThat(datos.saldo(e.ana().id())).isEqualByComparingTo(saldoAntes.subtract(cuota1.valor()));
        assertThat(apartado(e.ana().id())).isEqualByComparingTo("0");
    }

    @Test
    void moraYEscalamientoAlAvalHastaQueSusVentasCubrenLoVencido() {
        var e = prestamoConAval();
        sinSaldo(e.ana().id());
        var v1 = vencimiento(e.ana().id(), 1);

        cobranza.ejecutarCobroDiario(v1);
        var p = prestamo(e.ana().id());
        assertThat(p.estado()).isEqualTo(EstadoPrestamo.EN_MORA);
        assertThat(p.cuotasVencidas()).isEqualTo(1);

        cobranza.ejecutarCobroDiario(v1.plusDays(8));
        assertThat(estadoAval(e.prestamoId())).isEqualTo("ACEPTADO");
        assertThat(prestamo(e.ana().id()).cuotas().getFirst().getInteresMoratorio().esCero()).isFalse();

        cobranza.ejecutarCobroDiario(v1.plusDays(9));
        assertThat(jdbc.queryForObject("""
                select a.fecha_aviso is not null from credito.aval a join credito.prestamo p on p.oferta_id = a.oferta_id
                where p.id = ? and a.estado = 'ACEPTADO'""", Boolean.class, e.prestamoId())).isTrue();
        assertThat(consultar.panel(e.luis().id()).garantias()).singleElement()
                .satisfies(g -> assertThat(g.cuotasVencidas()).isGreaterThan(0));

        cobranza.ejecutarCobroDiario(v1.plusDays(15));
        assertThat(estadoAval(e.prestamoId())).isEqualTo("ACTIVADO");
        assertThat(jdbc.queryForObject("select nivel from evaluacion.historial_crediticio where usuario_id = ?",
                Integer.class, e.ana().id().valor())).isEqualTo(1);

        var vencido = prestamo(e.ana().id()).vencido();
        leCompran(e.luis(), "3000");
        assertThat(pagos(e.prestamoId(), "AVAL")).isGreaterThan(0);
        var despues = prestamo(e.ana().id());
        if (despues != null) {
            assertThat(despues.vencido().menorQue(vencido)).isTrue();
        }
    }

    @Test
    void pagoManualYPagoAnticipadoTotal() {
        var e = prestamoConAval();
        leCompran(e.ana(), "500");
        var ref = UUID.randomUUID().toString();
        ok(pagos.pagarCuotas(e.ana().id(), ref));
        ok(pagos.pagarCuotas(e.ana().id(), ref));
        assertThat(pagos(e.prestamoId(), "DEUDOR")).isEqualTo(1);
        assertThat(prestamo(e.ana().id()).cuotas().getFirst().getEstado().name()).isEqualTo("PAGADA");

        var vista = ok(pagos.prepararAnticipado(e.ana().id()));
        ok(pagos.pagarAnticipado(e.ana().id(), ModalidadPago.TOTAL, Dinero.CERO, vista.referencia()));

        assertThat(consultar.panel(e.ana().id()).prestamo()).isNull();
        assertThat(jdbc.queryForObject("select estado from credito.prestamo where id = ?", String.class, e.prestamoId()))
                .isEqualTo("PAGADO");
        assertThat(jdbc.queryForObject("""
                select a.estado from credito.aval a join credito.prestamo p on p.oferta_id = a.oferta_id
                where p.id = ? and a.estado in ('LIBERADO', 'ACEPTADO')""", String.class, e.prestamoId()))
                .isEqualTo(EstadoAval.LIBERADO.name());
        assertThat(jdbc.queryForObject("select nivel from evaluacion.historial_crediticio where usuario_id = ?",
                Integer.class, e.ana().id().valor())).isEqualTo(2);
        assertThat(apartado(e.ana().id())).isEqualByComparingTo("0");
        assertThat(consultar.panel(e.luis().id()).garantias()).isEmpty();
    }

    @Test
    void pagoAnticipadoParcialReprogramaElCronograma() {
        var e = prestamoConAval();
        var antes = prestamo(e.ana().id());
        var vista = ok(pagos.prepararAnticipado(e.ana().id()));
        ok(pagos.pagarAnticipado(e.ana().id(), ModalidadPago.REDUCIR_PLAZO, Dinero.de("300"), vista.referencia()));
        var despues = prestamo(e.ana().id());
        assertThat(despues.capitalPendiente()).isEqualTo(antes.capitalPendiente().restar(Dinero.de("300")));
        assertThat(despues.cuotas().stream().filter(c -> c.getEstado().name().equals("ANULADA")).count()).isGreaterThan(0);
        assertThat(jdbc.queryForObject("select modalidad from credito.pago where prestamo_id = ? and tipo = 'ANTICIPADO'",
                String.class, e.prestamoId())).isEqualTo("REDUCIR_PLAZO");
    }

    @Test
    void revocarElCargoAutomatico() {
        var e = prestamoConAval();
        leCompran(e.ana(), "500");
        assertThat(apartado(e.ana().id())).isEqualByComparingTo("40.00");

        assertThat(ok(pagos.revocarCargo(e.ana().id()))).isEqualTo(Dinero.de("40"));
        assertThat(apartado(e.ana().id())).isEqualByComparingTo("0");
        leCompran(e.ana(), "500");
        assertThat(apartado(e.ana().id())).isEqualByComparingTo("0");

        cobranza.ejecutarCobroDiario(vencimiento(e.ana().id(), 1));
        assertThat(prestamo(e.ana().id()).estado()).isEqualTo(EstadoPrestamo.EN_MORA);
        assertThat(pagos(e.prestamoId(), "DEUDOR")).isZero();
    }

    @Test
    void laTareaDiariaVenceInvitacionesYOfertas() {
        var ana = datos.cliente('0');
        datos.conHistoriaDeCobros(ana.id());
        var r = ok(solicitar.solicitar(ana.id(), Dinero.de("1000"), 8, Destino.OTRO, true));
        ok(avales.designar(ana.id(), r.ofertaId(), datos.cliente('5').celular()));
        jdbc.update("update credito.aval set fecha_invitacion = now() - interval '49 hours' where oferta_id = ?", r.ofertaId());
        jdbc.update("update credito.oferta set fecha = now() - interval '9 days', vigente_hasta = current_date - 2 where id = ?",
                r.ofertaId());

        var resumen = cobranza.ejecutarCobroDiario(LocalDate.now(clock));
        assertThat(resumen.ofertasActualizadas()).isGreaterThanOrEqualTo(1);
        assertThat(jdbc.queryForObject("select estado from credito.aval where oferta_id = ?", String.class, r.ofertaId()))
                .isEqualTo("RECHAZADO");
        assertThat(jdbc.queryForObject("select estado from credito.oferta where id = ?", String.class, r.ofertaId()))
                .isEqualTo("VENCIDA");
    }
}
