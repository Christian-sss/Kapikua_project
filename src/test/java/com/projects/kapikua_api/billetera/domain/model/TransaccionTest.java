package com.projects.kapikua_api.billetera.domain.model;

import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.UsuarioId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransaccionTest {
    static final OffsetDateTime AHORA = OffsetDateTime.of(2026, 11, 9, 10, 0, 0, 0, ZoneOffset.ofHours(-5));
    static final Dinero CINCUENTA = Dinero.de("50");

    static Billetera billetera(long id, String saldo) {
        return Billetera.reconstituir(id, new UsuarioId(id * 10), new Celular("98765432" + id), null, Dinero.de(saldo),
                Dinero.CERO, EstadoBilletera.ACTIVA, null, AHORA, 0L);
    }

    @Test
    void unaTransferenciaDebitaAlOrigenYAcreditaAlDestinoConSusSaldosPosteriores() {
        var tx = Transaccion.entreBilleteras(TipoTransaccion.TRANSFERENCIA, billetera(1, "50"), billetera(2, "150"),
                CINCUENTA, "ref-1", "Almuerzo", AHORA);
        assertThat(tx.movimientos()).containsExactly(
                new Transaccion.Movimiento(1L, new BigDecimal("-50.00"), Dinero.de("50")),
                new Transaccion.Movimiento(2L, new BigDecimal("50.00"), Dinero.de("150")));
        assertThat(tx.movimientoDe(2L)).map(Transaccion.Movimiento::monto).contains(new BigDecimal("50.00"));
        assertThat(tx.movimientoDe(99L)).isEmpty();
    }

    @Test
    void recargaYDesembolsoSoloTienenElMovimientoDeEntrada() {
        var recarga = Transaccion.recarga(billetera(2, "50"), CINCUENTA, "ref-r", AHORA);
        var desembolso = Transaccion.desembolso(billetera(2, "50"), CINCUENTA, "ref-d", "Crédito EFA-C-1", AHORA);
        assertThat(recarga.tipo()).isEqualTo(TipoTransaccion.RECARGA);
        assertThat(desembolso.tipo()).isEqualTo(TipoTransaccion.DESEMBOLSO);
        assertThat(List.of(recarga, desembolso)).allSatisfy(tx -> assertThat(tx.movimientos()).singleElement()
                .satisfies(m -> assertThat(m.monto()).isPositive()));
    }

    @ParameterizedTest
    @EnumSource(value = TipoTransaccion.class, names = {"PAGO_CUOTA", "PAGO_ANTICIPADO"})
    void unPagoDeCreditoSoloTieneElMovimientoDeSalida(TipoTransaccion tipo) {
        var tx = Transaccion.pagoACredito(tipo, billetera(1, "20"), CINCUENTA, "ref-p", "Cuota 1", AHORA);
        assertThat(tx.movimientos()).singleElement().satisfies(m -> assertThat(m.monto()).isNegative());
    }

    @ParameterizedTest
    @EnumSource(value = TipoTransaccion.class, names = {"PAGO_CUOTA", "PAGO_ANTICIPADO"}, mode = EnumSource.Mode.EXCLUDE)
    void unPagoDeCreditoNoAceptaOtrosTipos(TipoTransaccion tipo) {
        assertThatThrownBy(() -> Transaccion.pagoACredito(tipo, billetera(1, "20"), CINCUENTA, "r", null, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @EnumSource(value = TipoTransaccion.class, names = {"TRANSFERENCIA", "PAGO_NEGOCIO"}, mode = EnumSource.Mode.EXCLUDE)
    void entreBilleterasSoloHayTransferenciasYPagosANegocio(TipoTransaccion tipo) {
        assertThat(tipo.cuentaComoPagoOTransferencia()).isFalse();
        assertThatThrownBy(() -> Transaccion.entreBilleteras(tipo, billetera(1, "0"), billetera(2, "0"), CINCUENTA, "r", null, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void losMovimientosNoSePuedenModificarDesdeAfuera() {
        var lista = new ArrayList<>(List.of(new Transaccion.Movimiento(1L, BigDecimal.TEN, Dinero.de("10"))));
        var tx = new Transaccion(null, TipoTransaccion.RECARGA, Dinero.de("10"), "r", null, AHORA, lista);
        lista.clear();
        assertThat(tx.movimientos()).hasSize(1);
        assertThatThrownBy(() -> tx.movimientos().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void conIdConservaTodoLoDemas() {
        var tx = Transaccion.recarga(billetera(2, "50"), CINCUENTA, "ref-r", AHORA);
        var guardada = tx.conId(77L);
        assertThat(guardada.id()).isEqualTo(77L);
        assertThat(guardada).usingRecursiveComparison().ignoringFields("id").isEqualTo(tx);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void laReferenciaEsObligatoria(String referencia) {
        assertThatThrownBy(() -> Transaccion.recarga(billetera(2, "50"), CINCUENTA, referencia, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void laReferenciaTieneHasta60CaracteresYElMontoNoEsCero() {
        assertThat(Transaccion.recarga(billetera(2, "50"), CINCUENTA, "r".repeat(60), AHORA).referencia()).hasSize(60);
        assertThatThrownBy(() -> Transaccion.recarga(billetera(2, "50"), CINCUENTA, "r".repeat(61), AHORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Transaccion.recarga(billetera(2, "50"), Dinero.CERO, "r", AHORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Transaccion.recarga(billetera(2, "50"), CINCUENTA, null, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elCelularTiene9DigitosYEmpiezaCon9() {
        assertThat(Celular.de(" 987 654 321 ")).contains(new Celular("987654321"));
        assertThat(Celular.de("887654321")).isEmpty();
        assertThat(Celular.de("98765432")).isEmpty();
        assertThat(Celular.de(null)).isEmpty();
        assertThatThrownBy(() -> new Celular("12345")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Celular(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elCelularEnmascaradoMuestraSoloElInicioYElFinal() {
        assertThat(new Celular("987654321").enmascarado()).isEqualTo("987 *** 321");
    }

    @Test
    void cadaErrorTieneUnCodigoUnicoYUnMensajeParaElUsuario() {
        List<ErrorBilletera> errores = List.of(
                new ErrorBilletera.BilleteraNoEncontrada(), new ErrorBilletera.BilleteraBloqueada(),
                new ErrorBilletera.DestinatarioNoEncontrado(), new ErrorBilletera.NoEsNegocio(),
                new ErrorBilletera.OperacionConSiMismo(), new ErrorBilletera.MontoInvalido(),
                new ErrorBilletera.SaldoInsuficiente(), new ErrorBilletera.TopePorOperacion(Dinero.de("1000")),
                new ErrorBilletera.TopeSaldoPropio(Dinero.de("500")), new ErrorBilletera.DestinoNoPuedeRecibir(),
                new ErrorBilletera.TopeRecargasMes(Dinero.de("20")), new ErrorBilletera.TopePagosMes(Dinero.de("30")),
                new ErrorBilletera.PagoRechazado("tarjeta vencida"), new ErrorBilletera.CodigoNoValido("Código vencido"),
                new ErrorBilletera.ReferenciaInvalida(), new ErrorBilletera.CelularInvalido(),
                new ErrorBilletera.OperacionConcurrente(), new ErrorBilletera.ParametrosNoDisponibles());
        assertThat(errores).hasSize(ErrorBilletera.class.getPermittedSubclasses().length);
        assertThat(errores).allSatisfy(e -> {
            assertThat(e.codigo()).matches("BI-\\d{2}");
            assertThat(e.mensaje()).isNotBlank();
        });
        assertThat(new HashSet<>(errores.stream().map(ErrorBilletera::codigo).toList())).hasSameSizeAs(errores);
        assertThat(new ErrorBilletera.TopeSaldoPropio(Dinero.de("500")).mensaje()).contains("S/ 500.00");
        assertThat(new ErrorBilletera.TopeRecargasMes(Dinero.de("20")).mensaje()).contains("S/ 20.00");
    }
}
