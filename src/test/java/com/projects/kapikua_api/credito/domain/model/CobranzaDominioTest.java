package com.projects.kapikua_api.credito.domain.model;

import com.projects.kapikua_api.credito.domain.service.CalculadoraFrancesa;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.credito.domain.event.AtrasoDetectado;
import com.projects.kapikua_api.credito.domain.event.PagoAplicado;
import com.projects.kapikua_api.credito.domain.event.PrestamoPagado;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CobranzaDominioTest {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final LocalDate DESEMBOLSO = LocalDate.of(2026, 11, 9);
    private static final OffsetDateTime AHORA = OffsetDateTime.of(2026, 11, 9, 10, 0, 0, 0, ZoneOffset.ofHours(-5));
    private static final UsuarioId ANA = new UsuarioId(1L);
    private static final BigDecimal MORATORIA = new BigDecimal("17.20");

    private static Prestamo prestamo() {
        var cronograma = CalculadoraFrancesa.cronograma(Dinero.de("800"), new BigDecimal("95"), 8, DESEMBOLSO);
        var cuotas = new ArrayList<Cuota>();
        for (var c : cronograma.cuotas()) {
            cuotas.add(Cuota.reconstituir((long) c.numero() * 10, c.numero(), c.vencimiento(), c.capital(), c.interes(),
                    Dinero.CERO, Dinero.CERO, Dinero.CERO, null, EstadoCuota.PENDIENTE));
        }
        return Prestamo.reconstituir(1L, 7L, ANA, "AYNI_SUR", "EFA-C-1", Dinero.de("800"), new BigDecimal("95"),
                new BigDecimal("95"), 8, new ConstanciaAceptacion(AHORA, "x", "HR-1.0"), true, AHORA, null,
                EstadoPrestamo.VIGENTE, AHORA, 0L, cuotas, false);
    }

    private static Cuota cuota(Prestamo p, int numero) {
        return p.getCuotas().get(numero - 1);
    }

    @Test
    void elPagoVaPrimeroAlMoratorioLuegoAlInteresYAlFinalAlCapital() {
        var c = Cuota.reconstituir(1L, 1, DESEMBOLSO, Dinero.de("100"), Dinero.de("9.40"), Dinero.de("1.20"),
                Dinero.CERO, Dinero.CERO, null, EstadoCuota.VENCIDA);
        assertThat(c.imputar(Dinero.de("5"))).isEqualTo(new Imputacion(Dinero.de("1.20"), Dinero.de("3.80"), Dinero.CERO));
        c.pagar(Dinero.de("5"), Dinero.CERO, DESEMBOLSO);
        assertThat(c.imputar(c.pendiente())).isEqualTo(new Imputacion(Dinero.CERO, Dinero.de("5.60"), Dinero.de("100")));
        c.pagar(c.pendiente(), Dinero.CERO, DESEMBOLSO.plusDays(1));
        assertThat(c.getEstado()).isEqualTo(EstadoCuota.PAGADA);
        assertThat(c.getFechaPago()).isEqualTo(DESEMBOLSO.plusDays(1));
    }

    @Test
    void laMoraEsInteresDiarioSobreElCapitalVencidoSinCapitalizarYNuncaBaja() {
        var p = prestamo();
        var c1 = cuota(p, 1);
        p.vencerImpagas(c1.getFechaVencimiento());
        assertThat(c1.getEstado()).isEqualTo(EstadoCuota.VENCIDA);
        assertThat(p.getEstado()).isEqualTo(EstadoPrestamo.EN_MORA);

        var diezDias = c1.getFechaVencimiento().plusDays(10);
        p.actualizarMora(diezDias, MORATORIA);
        var esperado = c1.getCapital().valor().multiply(new BigDecimal("0.000441")).multiply(BigDecimal.TEN);
        assertThat(c1.getInteresMoratorio().valor()).isCloseTo(esperado, within(new BigDecimal("0.02")));
        var antes = c1.getInteresMoratorio();
        p.actualizarMora(diezDias.minusDays(3), MORATORIA);
        assertThat(c1.getInteresMoratorio()).isEqualTo(antes);
        assertThat(p.diasAtraso(diezDias)).isEqualTo(10);
    }

    @Test
    void seRetieneElPorcentajeAutorizadoSoloHastaCubrirLaCuota() {
        var p = prestamo();
        var c1 = cuota(p, 1);
        assertThat(p.montoARetener(Dinero.de("500"))).isEqualTo(Dinero.de("40"));
        p.registrarRetencion(100L, Dinero.de("40"), AHORA);
        assertThat(c1.getMontoApartado()).isEqualTo(Dinero.de("40"));

        var falta = c1.pendiente().restar(Dinero.de("40"));
        assertThat(p.montoARetener(Dinero.de("5000"))).isEqualTo(falta);
        p.registrarRetencion(101L, falta, AHORA);
        assertThat(p.montoARetener(Dinero.de("500"))).isEqualTo(Dinero.CERO);
        assertThat(p.retencionesNuevas()).hasSize(2);
    }

    @Test
    void alVencerSeUsaPrimeroLoApartado() {
        var p = prestamo();
        var c1 = cuota(p, 1);
        p.registrarRetencion(100L, Dinero.de("40"), AHORA);
        var sobrante = p.aplicarPago(List.of(c1), c1.pendiente(), true, 900L, Origen.DEUDOR, true,
                c1.getFechaVencimiento(), AHORA);
        assertThat(sobrante).isEqualTo(Dinero.CERO);
        assertThat(c1.getEstado()).isEqualTo(EstadoCuota.PAGADA);
        assertThat(c1.getMontoApartado()).isEqualTo(Dinero.CERO);
        assertThat(p.pagosNuevos()).singleElement().satisfies(pago -> {
            assertThat(pago.automatico()).isTrue();
            assertThat(pago.imputacion().total()).isEqualTo(pago.monto());
        });
        assertThat(p.cuotaObjetivo().orElseThrow().getNumero()).isEqualTo(2);
    }

    @Test
    void conPagoParcialLaCuotaQuedaVencidaYElPrestamoEnMora() {
        var p = prestamo();
        var c1 = cuota(p, 1);
        p.aplicarPago(List.of(c1), Dinero.de("50"), false, 900L, Origen.DEUDOR, true, c1.getFechaVencimiento(), AHORA);
        p.vencerImpagas(c1.getFechaVencimiento());
        assertThat(c1.getEstado()).isEqualTo(EstadoCuota.VENCIDA);
        assertThat(p.totalVencido()).isEqualTo(c1.pendiente());
        assertThat(p.getEstado()).isEqualTo(EstadoPrestamo.EN_MORA);
    }

    @Test
    void elPagoAnticipadoTotalCancelaElCapitalYCierraElPrestamo() {
        var p = prestamo();
        var capital = p.montoAnticipadoTotal();
        assertThat(capital).isEqualTo(Dinero.de("800"));
        p.aplicarAnticipado(capital, ModalidadPago.TOTAL, Dinero.CERO, 901L, AHORA);
        assertThat(p.getEstado()).isEqualTo(EstadoPrestamo.PAGADO);
        assertThat(p.getCuotas()).allSatisfy(c -> assertThat(c.getEstado()).isEqualTo(EstadoCuota.ANULADA));
        assertThat(p.pagosNuevos()).singleElement().satisfies(pago -> {
            assertThat(pago.tipo()).isEqualTo(TipoPago.ANTICIPADO);
            assertThat(pago.imputacion()).isEqualTo(Imputacion.soloCapital(capital));
        });
    }

    @Test
    void reducirCuotaMantieneLasSemanasYBajaLaCuota() {
        var p = prestamo();
        var cuotaAntes = cuota(p, 1).total();
        p.aplicarAnticipado(Dinero.de("300"), ModalidadPago.REDUCIR_CUOTA, Dinero.CERO, 902L, AHORA);
        assertThat(p.getCuotas()).allSatisfy(c -> assertThat(c.getEstado()).isEqualTo(EstadoCuota.PENDIENTE));
        assertThat(p.capitalPendiente()).isEqualTo(Dinero.de("500"));
        assertThat(cuota(p, 1).total()).isLessThan(cuotaAntes);
    }

    @Test
    void reducirPlazoMantieneLaCuotaYAnulaLasUltimas() {
        var p = prestamo();
        var cuotaAntes = cuota(p, 1).total();
        p.aplicarAnticipado(Dinero.de("300"), ModalidadPago.REDUCIR_PLAZO, Dinero.CERO, 903L, AHORA);
        var vivas = p.getCuotas().stream().filter(c -> c.getEstado() == EstadoCuota.PENDIENTE).toList();
        assertThat(vivas.size()).isLessThan(8);
        assertThat(p.getCuotas().getLast().getEstado()).isEqualTo(EstadoCuota.ANULADA);
        assertThat(p.capitalPendiente()).isEqualTo(Dinero.de("500"));
        assertThat(vivas.getFirst().total()).isEqualTo(cuotaAntes);
    }

    @Test
    void noSeAdelantaConCuotasVencidasNiUnParcialMayorOIgualAlCapital() {
        var p = prestamo();
        assertThat(err(p.verificarAnticipado(Dinero.de("800"), ModalidadPago.REDUCIR_CUOTA)))
                .isInstanceOf(ErrorCredito.MontoAnticipadoInvalido.class);
        p.vencerImpagas(cuota(p, 1).getFechaVencimiento());
        assertThat(err(p.verificarAnticipado(Dinero.de("100"), ModalidadPago.REDUCIR_CUOTA)))
                .isInstanceOf(ErrorCredito.HayCuotasVencidas.class);
    }

    @Test
    void alRevocarElCargoSeLiberaLoApartadoYNoSeRetieneMas() {
        var p = prestamo();
        p.registrarRetencion(100L, Dinero.de("40"), AHORA);
        var liberado = p.revocarCargo(AHORA.plusDays(1));
        assertThat(((Result.Ok<Dinero, ErrorCredito>) liberado).valor()).isEqualTo(Dinero.de("40"));
        assertThat(p.isCargoAutorizado()).isFalse();
        assertThat(p.totalApartado()).isEqualTo(Dinero.CERO);
        assertThat(p.montoARetener(Dinero.de("500"))).isEqualTo(Dinero.CERO);
    }

    @Test
    void elAvalSeAvisaAlDiaNueveSeActivaAlQuinceYSeLiberaAlPagar() {
        var o = Oferta.crear(10L, ANA, AHORA, LIMA, 7);
        var luis = new UsuarioId(2L);
        o.designarAval(luis, true, 3, AHORA);
        o.responderAval(luis, true, 10, "x", AHORA);

        assertThat(o.escalarAval(8, 9, 15, AHORA)).isEqualTo(Oferta.Escalamiento.NINGUNO);
        assertThat(o.escalarAval(9, 9, 15, AHORA)).isEqualTo(Oferta.Escalamiento.AVISADO);
        assertThat(o.escalarAval(10, 9, 15, AHORA)).isEqualTo(Oferta.Escalamiento.NINGUNO);
        assertThat(o.escalarAval(15, 9, 15, AHORA)).isEqualTo(Oferta.Escalamiento.ACTIVADO);
        assertThat(o.avalAceptadoOActivo().orElseThrow().getEstado()).isEqualTo(EstadoAval.ACTIVADO);

        o.liberarAval(AHORA.plusDays(20));
        assertThat(o.getAvales().getFirst().getEstado()).isEqualTo(EstadoAval.LIBERADO);
    }

    private static ErrorCredito err(Result<?, ErrorCredito> r) {
        assertThat(r).isInstanceOf(Result.Err.class);
        return ((Result.Err<?, ErrorCredito>) r).error();
    }

    @Test
    void cadaPagoRegistraUnPagoAplicadoYPagarTodoRegistraPrestamoPagadoUnaSolaVez() {
        var p = prestamo();
        var c1 = cuota(p, 1);
        p.aplicarPago(List.of(c1), c1.pendiente(), false, 900L, Origen.DEUDOR, false, DESEMBOLSO, AHORA);
        assertThat(p.extraerEventos()).singleElement().isInstanceOfSatisfying(PagoAplicado.class, e -> {
            assertThat(e.codigoCredito()).isEqualTo("EFA-C-1");
            assertThat(e.monto()).isEqualTo(c1.total());
            assertThat(e.origen()).isEqualTo(Origen.DEUDOR);
        });
        assertThat(p.extraerEventos()).isEmpty();

        p.aplicarAnticipado(p.montoAnticipadoTotal(), ModalidadPago.TOTAL, Dinero.CERO, 901L, AHORA);
        var eventos = p.extraerEventos();
        assertThat(eventos).hasSize(2);
        assertThat(eventos.get(0)).isInstanceOf(PagoAplicado.class);
        assertThat(eventos.get(1)).isInstanceOfSatisfying(PrestamoPagado.class, e -> {
            assertThat(e.usuarioId()).isEqualTo(ANA);
            assertThat(e.ofertaId()).isEqualTo(7L);
            assertThat(e.pagadoPorAval()).isFalse();
        });
    }

    @Test
    void siElAvalPagoAlgunaCuotaElPrestamoPagadoLoIndica() {
        var p = prestamo();
        var c1 = cuota(p, 1);
        p.vencerImpagas(c1.getFechaVencimiento());
        p.aplicarPago(List.of(c1), c1.pendiente(), false, 900L, Origen.AVAL, false, c1.getFechaVencimiento(), AHORA);
        p.aplicarAnticipado(p.montoAnticipadoTotal(), ModalidadPago.TOTAL, Dinero.CERO, 901L, AHORA);
        assertThat(p.extraerEventos()).filteredOn(PrestamoPagado.class::isInstance).singleElement()
                .isInstanceOfSatisfying(PrestamoPagado.class, e -> assertThat(e.pagadoPorAval()).isTrue());
    }

    @Test
    void revisarAtrasoRegistraLosDiasSoloSiHayCuotasVencidas() {
        var p = prestamo();
        var c1 = cuota(p, 1);
        assertThat(p.revisarAtraso(c1.getFechaVencimiento().minusDays(1))).isZero();
        assertThat(p.extraerEventos()).isEmpty();
        assertThat(p.revisarAtraso(c1.getFechaVencimiento().plusDays(3))).isEqualTo(3);
        assertThat(p.extraerEventos()).singleElement().isInstanceOfSatisfying(AtrasoDetectado.class,
                e -> assertThat(e.dias()).isEqualTo(3));
        assertThat(p.getEstado()).isEqualTo(EstadoPrestamo.EN_MORA);
    }
}
