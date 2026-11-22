package com.projects.kapikua_api.credito.model;

import com.projects.kapikua_api.credito.model.CalculadoraFrancesa;

import com.projects.kapikua_api.credito.error.ErrorCredito;
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
                    Dinero.CERO, Dinero.CERO, Dinero.CERO, null, "PENDIENTE"));
        }
        return Prestamo.reconstituir(1L, 7L, ANA, "AYNI_SUR", "EFA-C-1", Dinero.de("800"), new BigDecimal("95"),
                new BigDecimal("95"), 8, new ConstanciaAceptacion(AHORA, "x", "HR-1.0"), true, AHORA, null,
                "VIGENTE", AHORA, 0L, cuotas, false);
    }

    private static Cuota cuota(Prestamo p, int numero) {
        return p.getCuotas().get(numero - 1);
    }

    @Test
    void elPagoVaPrimeroAlMoratorioLuegoAlInteresYAlFinalAlCapital() {
        var c = Cuota.reconstituir(1L, 1, DESEMBOLSO, Dinero.de("100"), Dinero.de("9.40"), Dinero.de("1.20"),
                Dinero.CERO, Dinero.CERO, null, "VENCIDA");
        var primerPago = ok(c.imputar(Dinero.de("5"), Dinero.CERO, DESEMBOLSO));
        assertThat(primerPago.imputacion()).isEqualTo(new Imputacion(Dinero.de("1.20"), Dinero.de("3.80"), Dinero.CERO));
        var segundoPago = ok(c.imputar(c.pendiente(), Dinero.CERO, DESEMBOLSO.plusDays(1)));
        assertThat(segundoPago.imputacion()).isEqualTo(new Imputacion(Dinero.CERO, Dinero.de("5.60"), Dinero.de("100")));
        assertThat(c.getEstado()).isEqualTo("PAGADA");
        assertThat(c.getFechaPago()).isEqualTo(DESEMBOLSO.plusDays(1));
    }

    @Test
    void laMoraEsInteresDiarioSobreElCapitalVencidoSinCapitalizarYNuncaBaja() {
        var p = prestamo();
        var c1 = cuota(p, 1);
        p.vencerImpagas(c1.getFechaVencimiento(), AHORA);
        assertThat(c1.getEstado()).isEqualTo("VENCIDA");
        assertThat(p.getEstado()).isEqualTo("EN_MORA");

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
        assertThat(ok(sobrante)).isEqualTo(Dinero.CERO);
        assertThat(c1.getEstado()).isEqualTo("PAGADA");
        assertThat(c1.getMontoApartado()).isEqualTo(Dinero.CERO);
        assertThat(p.pagosNuevos()).singleElement().satisfies(pago -> {
            assertThat(pago.automatico()).isTrue();
            assertThat(pago.modalidad().codigo()).isEqualTo("REGULAR");
            assertThat(pago.imputacion().total()).isEqualTo(pago.monto());
        });
        assertThat(p.cuotaObjetivo().orElseThrow().getNumero()).isEqualTo(2);
    }

    @Test
    void conPagoParcialLaCuotaQuedaVencidaYElPrestamoEnMora() {
        var p = prestamo();
        var c1 = cuota(p, 1);
        p.aplicarPago(List.of(c1), Dinero.de("50"), false, 900L, Origen.DEUDOR, true, c1.getFechaVencimiento(), AHORA);
        p.vencerImpagas(c1.getFechaVencimiento(), AHORA);
        assertThat(c1.getEstado()).isEqualTo("VENCIDA");
        assertThat(p.totalVencido()).isEqualTo(c1.pendiente());
        assertThat(p.getEstado()).isEqualTo("EN_MORA");
    }

    @Test
    void elPagoAnticipadoTotalCancelaElCapitalYCierraElPrestamo() {
        var p = prestamo();
        var capital = p.montoAnticipadoTotal();
        assertThat(capital).isEqualTo(Dinero.de("800"));
        p.aplicarAnticipado(capital, ModalidadPago.TOTAL, Dinero.CERO, 901L, AHORA);
        assertThat(p.getEstado()).isEqualTo("PAGADO");
        assertThat(p.getCuotas()).allSatisfy(c -> assertThat(c.getEstado()).isEqualTo("ANULADA"));
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
        assertThat(p.getCuotas()).allSatisfy(c -> assertThat(c.getEstado()).isEqualTo("PENDIENTE"));
        assertThat(p.capitalPendiente()).isEqualTo(Dinero.de("500"));
        assertThat(cuota(p, 1).total()).isLessThan(cuotaAntes);
    }

    @Test
    void reducirPlazoMantieneLaCuotaYAnulaLasUltimas() {
        var p = prestamo();
        var cuotaAntes = cuota(p, 1).total();
        p.aplicarAnticipado(Dinero.de("300"), ModalidadPago.REDUCIR_PLAZO, Dinero.CERO, 903L, AHORA);
        var vivas = p.getCuotas().stream().filter(Cuota::esPendiente).toList();
        assertThat(vivas.size()).isLessThan(8);
        assertThat(p.getCuotas().getLast().getEstado()).isEqualTo("ANULADA");
        assertThat(p.capitalPendiente()).isEqualTo(Dinero.de("500"));
        assertThat(vivas.getFirst().total()).isEqualTo(cuotaAntes);
    }

    @Test
    void noSeAdelantaConCuotasVencidasNiUnParcialMayorOIgualAlCapital() {
        var p = prestamo();
        assertThat(err(p.verificarAnticipado(Dinero.de("800"), ModalidadPago.REDUCIR_CUOTA)))
                .isInstanceOf(ErrorCredito.MontoAnticipadoInvalido.class);
        p.vencerImpagas(cuota(p, 1).getFechaVencimiento(), AHORA);
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
        assertThat(o.avalAceptadoOActivo().orElseThrow().getEstado()).isEqualTo("ACTIVADO");

        o.liberarAval(AHORA.plusDays(20));
        assertThat(o.getAvales().getFirst().getEstado()).isEqualTo("LIBERADO");
    }

    private static ErrorCredito err(Result<?, ErrorCredito> r) {
        assertThat(r).isInstanceOf(Result.Err.class);
        return ((Result.Err<?, ErrorCredito>) r).error();
    }

    private static <T> T ok(Result<T, ErrorCredito> resultado) {
        if (resultado instanceof Result.Err<T, ErrorCredito> err) throw new AssertionError(err.error().mensaje());
        return ((Result.Ok<T, ErrorCredito>) resultado).valor();
    }

    @Test
    void unaCuotaVencidaRechazaLaAnulacionYUnaPagadaRechazaOtroPago() {
        var c = cuota(prestamo(), 1);
        var vencimiento = c.getFechaVencimiento();
        var fecha = AHORA.plusDays(7);
        assertThat(c.vencer(vencimiento.minusDays(1), fecha).esOk()).isFalse();
        var vencida = ok(c.vencer(vencimiento, fecha));
        assertThat(vencida.anterior()).isEqualTo("PENDIENTE");
        assertThat(vencida.nuevo()).isEqualTo("VENCIDA");
        assertThat(vencida.fecha()).isEqualTo(fecha);
        assertThat(c.anular(fecha).esOk()).isFalse();
        var pendiente = c.pendiente();
        ok(c.imputar(pendiente, Dinero.CERO, vencimiento));
        assertThat(c.imputar(Dinero.de("1"), Dinero.CERO, vencimiento).esOk()).isFalse();
        assertThat(c.getMontoPagado()).isEqualTo(pendiente);
        assertThat(c.getEstado()).isEqualTo("PAGADA");
    }

    @Test
    void unPagoRegularExcesivoNoModificaLasCuotasNiRegistraPagos() {
        var p = prestamo();
        var c1 = cuota(p, 1);
        var monto = c1.pendiente().sumar(Dinero.de("1"));
        assertThat(p.aplicarPago(List.of(c1), monto, false, 900L, Origen.DEUDOR, false, DESEMBOLSO, AHORA).esOk())
                .isFalse();
        assertThat(c1.getMontoPagado()).isEqualTo(Dinero.CERO);
        assertThat(p.pagosNuevos()).isEmpty();
        assertThat(p.getEstado()).isEqualTo("VIGENTE");
    }

    @Test
    void unaLiquidacionIncompletaSeRechazaYElPrestamoPagadoEsFinal() {
        var p = prestamo();
        assertThat(err(p.cerrar(AHORA))).isInstanceOf(ErrorCredito.PrestamoConSaldoPendiente.class);
        assertThat(p.aplicarAnticipado(Dinero.de("1"), ModalidadPago.TOTAL, Dinero.CERO, 901L, AHORA).esOk())
                .isFalse();
        assertThat(p.getCuotas()).allMatch(Cuota::esPendiente);
        assertThat(p.pagosNuevos()).isEmpty();

        ok(p.aplicarAnticipado(p.montoAnticipadoTotal(), ModalidadPago.TOTAL, Dinero.CERO, 902L, AHORA));
        assertThat(p.marcarEnMora(AHORA).esOk()).isFalse();
        assertThat(p.cerrar(AHORA).esOk()).isFalse();
        assertThat(p.registrarRetencion(903L, Dinero.de("1"), AHORA).esOk()).isFalse();
        assertThat(p.getEstado()).isEqualTo("PAGADO");
        assertThat(p.pagosNuevos()).singleElement().extracting(Pago::tipo).isEqualTo(TipoPago.ANTICIPADO);
        assertThat(p.retencionesNuevas()).isEmpty();
    }

    @Test
    void cadaPagoQuedaRegistradoYPagarTodoDejaElPrestamoPagado() {
        var p = prestamo();
        var c1 = cuota(p, 1);
        p.aplicarPago(List.of(c1), c1.pendiente(), false, 900L, Origen.DEUDOR, false, DESEMBOLSO, AHORA);
        assertThat(p.pagosNuevos()).singleElement().satisfies(pago -> {
            assertThat(pago.tipo()).isEqualTo(TipoPago.CUOTA);
            assertThat(pago.cuotaId()).isEqualTo(c1.getId());
            assertThat(pago.transaccionId()).isEqualTo(900L);
            assertThat(pago.monto()).isEqualTo(c1.total());
            assertThat(pago.origen()).isEqualTo(Origen.DEUDOR);
        });
        assertThat(p.getEstado()).isEqualTo("VIGENTE");

        p.aplicarAnticipado(p.montoAnticipadoTotal(), ModalidadPago.TOTAL, Dinero.CERO, 901L, AHORA);
        assertThat(p.pagosNuevos()).hasSize(2);
        assertThat(p.pagosNuevos().get(1).tipo()).isEqualTo(TipoPago.ANTICIPADO);
        assertThat(p.estaPagado()).isTrue();
        assertThat(p.isConPagosDeAval()).isFalse();
    }

    @Test
    void siElAvalPagoAlgunaCuotaElPrestamoPagadoLoIndica() {
        var p = prestamo();
        var c1 = cuota(p, 1);
        p.vencerImpagas(c1.getFechaVencimiento(), AHORA);
        ok(p.aplicarPago(List.of(c1), c1.pendiente(), false, 900L, Origen.AVAL, false, c1.getFechaVencimiento(), AHORA));
        assertThat(p.getEstado()).isEqualTo("VIGENTE");
        ok(p.aplicarAnticipado(p.montoAnticipadoTotal(), ModalidadPago.TOTAL, Dinero.CERO, 901L, AHORA));
        assertThat(p.estaPagado()).isTrue();
        assertThat(p.isConPagosDeAval()).isTrue();
    }

    @Test
    void revisarAtrasoDevuelveLosDiasSoloSiHayCuotasVencidas() {
        var p = prestamo();
        var c1 = cuota(p, 1);
        assertThat(p.revisarAtraso(c1.getFechaVencimiento().minusDays(1), AHORA)).isZero();
        assertThat(p.getEstado()).isEqualTo("VIGENTE");
        assertThat(p.revisarAtraso(c1.getFechaVencimiento().plusDays(3), AHORA)).isEqualTo(3);
        assertThat(p.getEstado()).isEqualTo("EN_MORA");
    }
}
