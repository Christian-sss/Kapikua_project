package com.projects.kapikua_api.credito;

import com.projects.kapikua_api.DatosDePrueba;
import com.projects.kapikua_api.credito.application.port.in.AceptarOfertaUseCase;
import com.projects.kapikua_api.credito.application.port.in.AceptarOfertaUseCase.HojaResumen;
import com.projects.kapikua_api.credito.application.port.in.ConsultarCreditoUseCase;
import com.projects.kapikua_api.credito.application.port.in.ConsultarCreditoUseCase.DetalleOferta;
import com.projects.kapikua_api.credito.application.port.in.GestionarAvalUseCase;
import com.projects.kapikua_api.credito.application.port.in.GestionarAvalUseCase.InvitacionAval;
import com.projects.kapikua_api.credito.application.port.in.SimularCreditoUseCase;
import com.projects.kapikua_api.credito.application.port.in.SolicitarCreditoUseCase;
import com.projects.kapikua_api.credito.application.port.in.SolicitarCreditoUseCase.ResultadoSolicitud;
import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.credito.domain.model.EstadoAval;
import com.projects.kapikua_api.credito.domain.model.EstadoOferta;
import com.projects.kapikua_api.credito.domain.model.EstadoPrestamo;
import com.projects.kapikua_api.evaluacion.domain.model.Destino;
import com.projects.kapikua_api.evaluacion.domain.model.Recomendacion;
import com.projects.kapikua_api.identidad.application.port.in.RegistrarUsuarioUseCase;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.puerto.EjecutorTransaccional;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "KAPIKUA_DB_PRUEBAS", matches = "jdbc:postgresql:.+")
class CreditoIntegracionTest {
    @DynamicPropertySource
    static void base(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", () -> System.getenv("KAPIKUA_DB_PRUEBAS"));
    }

    @Autowired RegistrarUsuarioUseCase registrar;
    @Autowired SimularCreditoUseCase simular;
    @Autowired SolicitarCreditoUseCase solicitar;
    @Autowired GestionarAvalUseCase avales;
    @Autowired AceptarOfertaUseCase aceptar;
    @Autowired ConsultarCreditoUseCase consultar;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;
    @Autowired EjecutorTransaccional ejecutor;
    DatosDePrueba datos;

    @BeforeEach
    void preparar() {
        datos = new DatosDePrueba(registrar, jdbc, clock);
    }

    private static <T> T ok(Result<T, ErrorCredito> r) {
        if (r instanceof Result.Err<T, ErrorCredito> e) throw new AssertionError("Se esperaba Ok y fue " + e.error());
        return ((Result.Ok<T, ErrorCredito>) r).valor();
    }

    private static ErrorCredito err(Result<?, ErrorCredito> r) {
        assertThat(r).isInstanceOf(Result.Err.class);
        return ((Result.Err<?, ErrorCredito>) r).error();
    }

    private DatosDePrueba.Cliente clienteElegible(char digito) {
        var c = datos.cliente(digito);
        datos.conHistoriaDeCobros(c.id());
        return c;
    }

    private ResultadoSolicitud solicitar(UsuarioId u, String monto, int cuotas) {
        return ok(solicitar.solicitar(u, Dinero.de(monto), cuotas, Destino.CAPITAL_TRABAJO, true));
    }

    private DetalleOferta oferta(UsuarioId u, Long id) {
        return ok(consultar.oferta(u, id));
    }

    private void avalAcepta(DatosDePrueba.Cliente deudor, Long ofertaId, DatosDePrueba.Cliente aval) {
        ok(avales.designar(deudor.id(), ofertaId, aval.celular()));
        InvitacionAval inv = ok(avales.invitacion(aval.id(), ofertaId));
        ok(avales.responder(aval.id(), ofertaId, true, inv.codigoMostrado(), "Chrome/Android"));
    }

    @Test
    void simuladorSegunElNivel() {
        var ana = clienteElegible('0');
        var op = ok(simular.opciones(ana.id()));
        assertThat(op.nivel()).isEqualTo(1);
        assertThat(op.cuotasMinimas()).isEqualTo(4);
        assertThat(op.cuotasMaximas()).isEqualTo(8);
        assertThat(op.tea()).isEqualByComparingTo("95");

        var s = ok(simular.simular(ana.id(), Dinero.de("1000"), 8));
        assertThat(s.cronograma()).hasSize(8);
        assertThat(s.tcea()).isEqualByComparingTo(s.tea());
        assertThat(err(simular.simular(ana.id(), Dinero.de("1000"), 9))).isEqualTo(new ErrorCredito.CuotasFueraDeRango(4, 8));
        assertThat(err(simular.simular(ana.id(), Dinero.de("3300.01"), 8))).isInstanceOf(ErrorCredito.MontoInvalido.class);
    }

    @Test
    void cicloCompletoConAvalHastaElDesembolso() {
        var ana = clienteElegible('0');
        var luis = datos.cliente('5');

        var r = solicitar(ana.id(), "3000", 8);
        assertThat(r.recomendacion()).isEqualTo(Recomendacion.APTO);
        assertThat(r.requiereAval()).isTrue();
        var o = oferta(ana.id(), r.ofertaId());
        assertThat(o.estado()).isEqualTo(EstadoOferta.PENDIENTE_EFA);
        assertThat(o.puedeDesignarAval()).isTrue();
        assertThat(o.puedeAceptar()).isFalse();

        avalAcepta(ana, r.ofertaId(), luis);
        o = oferta(ana.id(), r.ofertaId());
        assertThat(o.estado()).isEqualTo(EstadoOferta.CONFIRMADA);
        assertThat(o.avales()).singleElement().satisfies(a -> assertThat(a.estado()).isEqualTo(EstadoAval.ACEPTADO));

        HojaResumen hoja = ok(aceptar.hojaResumen(ana.id(), r.ofertaId()));
        assertThat(hoja.tasaMoratoriaMaxima()).isEqualByComparingTo("17.20");
        assertThat(hoja.cronograma()).hasSize(8);
        assertThat(hoja.aval()).isNotNull();

        var saldoAntes = datos.saldo(ana.id());
        var prestamoId = ok(aceptar.aceptar(ana.id(), r.ofertaId(), hoja.codigoMostrado(), "Chrome/Android", true));

        var panel = consultar.panel(ana.id());
        assertThat(panel.prestamo().id()).isEqualTo(prestamoId);
        assertThat(panel.prestamo().estado()).isEqualTo(EstadoPrestamo.VIGENTE);
        assertThat(panel.prestamo().cuotas()).hasSize(8);
        assertThat(panel.prestamo().capitalPendiente()).isEqualTo(hoja.condiciones().monto());
        assertThat(datos.saldo(ana.id())).isEqualByComparingTo(saldoAntes.add(hoja.condiciones().monto().valor()));
        assertThat(jdbc.queryForObject("""
                select count(*) from billetera.transaccion t join billetera.movimiento m on m.transaccion_id = t.id
                join billetera.billetera b on b.id = m.billetera_id where t.tipo = 'DESEMBOLSO' and b.usuario_id = ?""",
                Integer.class, ana.id().valor())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from evaluacion.historial_crediticio where usuario_id = ?",
                Integer.class, ana.id().valor())).isEqualTo(1);

        assertThat(err(solicitar.solicitar(ana.id(), Dinero.de("500"), 8, Destino.OTRO, true)))
                .isInstanceOf(ErrorCredito.PrestamoActivoExistente.class);
        var pepe = clienteElegible('1');
        var r2 = solicitar(pepe.id(), "1000", 8);
        assertThat(err(avales.designar(pepe.id(), r2.ofertaId(), luis.celular()))).isInstanceOf(ErrorCredito.AvalNoCalifica.class);
    }

    @Test
    void sinAutorizarLosDatosNoSeEvalua() {
        var ana = clienteElegible('0');
        assertThat(err(solicitar.solicitar(ana.id(), Dinero.de("1000"), 8, Destino.OTRO, false)))
                .isInstanceOf(ErrorCredito.ConsentimientoRequerido.class);
    }

    @Test
    void sinUsoMinimoSeExplicaQueFalta() {
        var nueva = datos.cliente('0');
        assertThat(err(solicitar.solicitar(nueva.id(), Dinero.de("1000"), 8, Destino.OTRO, true)).mensaje())
                .contains("30 días");
    }

    @Test
    void noAptoNoCreaOferta() {
        var deficiente = clienteElegible('8');
        var r = solicitar(deficiente.id(), "1000", 8);
        assertThat(r.recomendacion()).isEqualTo(Recomendacion.NO_APTO);
        assertThat(r.ofertaId()).isNull();
        assertThat(r.motivo()).contains("DEFICIENTE");
    }

    @Test
    void reglasDelAval() {
        var ana = clienteElegible('0');
        var r = solicitar(ana.id(), "1000", 8);

        assertThat(err(avales.designar(ana.id(), r.ofertaId(), ana.celular()))).isInstanceOf(ErrorCredito.AvalMismoDeudor.class);
        assertThat(err(avales.designar(ana.id(), r.ofertaId(), "999999999"))).isInstanceOf(ErrorCredito.AvalNoEncontrado.class);
        var sinHistorial = datos.cliente('1');
        assertThat(err(avales.designar(ana.id(), r.ofertaId(), sinHistorial.celular()))).isInstanceOf(ErrorCredito.AvalNoCalifica.class);

        var rosa = datos.cliente('4');
        ok(avales.designar(ana.id(), r.ofertaId(), rosa.celular()));
        assertThat(err(avales.designar(ana.id(), r.ofertaId(), datos.cliente('5').celular()))).isInstanceOf(ErrorCredito.AvalEnCurso.class);
        ok(avales.responder(rosa.id(), r.ofertaId(), false, null, null));
        assertThat(err(avales.designar(ana.id(), r.ofertaId(), rosa.celular()))).isInstanceOf(ErrorCredito.AvalYaInvitado.class);
        ok(avales.designar(ana.id(), r.ofertaId(), datos.cliente('5').celular()));
        assertThat(consultar.panel(rosa.id()).invitaciones()).isEmpty();
    }

    @Test
    void avalesCruzadosNoSePermiten() {
        var ana = clienteElegible('4');
        var luis = clienteElegible('5');
        var oAna = solicitar(ana.id(), "1000", 8);
        ok(avales.designar(ana.id(), oAna.ofertaId(), luis.celular()));
        var oLuis = solicitar(luis.id(), "1000", 8);
        assertThat(err(avales.designar(luis.id(), oLuis.ofertaId(), ana.celular()))).isInstanceOf(ErrorCredito.AvalCruzado.class);
    }

    @Test
    void elAvalAceptaSoloConSuCodigo() {
        var ana = clienteElegible('0');
        var luis = datos.cliente('5');
        var r = solicitar(ana.id(), "1000", 8);
        ok(avales.designar(ana.id(), r.ofertaId(), luis.celular()));
        ok(avales.invitacion(luis.id(), r.ofertaId()));
        assertThat(err(avales.responder(luis.id(), r.ofertaId(), true, "000000x", "x"))).isInstanceOf(ErrorCredito.CodigoNoValido.class);
        assertThat(oferta(ana.id(), r.ofertaId()).estado()).isEqualTo(EstadoOferta.PENDIENTE_EFA);
        assertThat(err(avales.invitacion(datos.cliente('5').id(), r.ofertaId()))).isInstanceOf(ErrorCredito.OfertaNoEncontrada.class);
    }

    @Test
    void laEntidadAliadaPuedeDenegar() {
        var ana = clienteElegible('3');
        var r = solicitar(ana.id(), "1000", 8);
        avalAcepta(ana, r.ofertaId(), datos.cliente('5'));
        var o = oferta(ana.id(), r.ofertaId());
        assertThat(o.estado()).isEqualTo(EstadoOferta.DENEGADA);
        assertThat(o.motivoDenegacion()).contains("política interna");
        assertThat(err(aceptar.hojaResumen(ana.id(), r.ofertaId()))).isInstanceOf(ErrorCredito.OfertaEstadoInvalido.class);
    }

    @Test
    void sinCodigoValidoOSinAutorizarElCargoNoSeDesembolsa() {
        var ana = clienteElegible('0');
        var r = solicitar(ana.id(), "1000", 8);
        avalAcepta(ana, r.ofertaId(), datos.cliente('5'));
        ok(aceptar.hojaResumen(ana.id(), r.ofertaId()));

        assertThat(err(aceptar.aceptar(ana.id(), r.ofertaId(), "x", "x", false))).isInstanceOf(ErrorCredito.CargoNoAutorizado.class);
        assertThat(err(aceptar.aceptar(ana.id(), r.ofertaId(), "000000x", "x", true))).isInstanceOf(ErrorCredito.CodigoNoValido.class);
        assertThat(consultar.panel(ana.id()).prestamo()).isNull();
        assertThat(oferta(ana.id(), r.ofertaId()).estado()).isEqualTo(EstadoOferta.CONFIRMADA);
    }

    @Test
    void elDesembolsoRespetaElSaldoMaximo() {
        var ana = clienteElegible('0');
        var r = solicitar(ana.id(), "3000", 8);
        avalAcepta(ana, r.ofertaId(), datos.cliente('5'));
        var hoja = ok(aceptar.hojaResumen(ana.id(), r.ofertaId()));
        jdbc.update("update billetera.billetera set saldo = 10000, version = version + 1 where usuario_id = ?", ana.id().valor());

        var e = err(aceptar.aceptar(ana.id(), r.ofertaId(), hoja.codigoMostrado(), "x", true));
        assertThat(e).isInstanceOf(ErrorCredito.DesembolsoNoPermitido.class);
        assertThat(consultar.panel(ana.id()).prestamo()).isNull();
        assertThat(datos.saldo(ana.id())).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(oferta(ana.id(), r.ofertaId()).estado()).isEqualTo(EstadoOferta.CONFIRMADA);
    }

    @Test
    void unErrDespuesDeEscribirRevierteLaTransaccion() {
        var ana = datos.cliente('0');
        var r = ejecutor.ejecutarResultado(() -> {
            jdbc.update("update billetera.billetera set saldo = 777 where usuario_id = ?", ana.id().valor());
            return Result.<Void, ErrorCredito>err(new ErrorCredito.EfaNoDisponible());
        });
        assertThat(r).isInstanceOf(Result.Err.class);
        assertThat(datos.saldo(ana.id())).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void sinAvalEnNivelTresSeConfirmaDeInmediato() {
        var pedro = clienteElegible('0');
        jdbc.update("insert into evaluacion.historial_crediticio (usuario_id, nivel) values (?, 3)", pedro.id().valor());
        var r = solicitar(pedro.id(), "1000", 16);
        assertThat(r.requiereAval()).isFalse();
        var o = oferta(pedro.id(), r.ofertaId());
        assertThat(o.estado()).isEqualTo(EstadoOferta.CONFIRMADA);
        assertThat(o.puedeAceptar()).isTrue();
        assertThat(o.tea()).isEqualByComparingTo("80");
    }
}
