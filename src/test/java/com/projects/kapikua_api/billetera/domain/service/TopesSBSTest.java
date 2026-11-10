package com.projects.kapikua_api.billetera.domain.service;

import com.projects.kapikua_api.billetera.domain.model.*;

import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TopesSBSTest {
    private static final Topes TOPES = Topes.desdeUit(new BigDecimal("5500"), new BigDecimal("0.6"),
            new BigDecimal("1.9"), new BigDecimal("1.9"), new BigDecimal("2.8"));
    private static final OffsetDateTime AHORA = OffsetDateTime.of(2026, 11, 9, 10, 0, 0, 0, ZoneOffset.ofHours(-5));

    private static Billetera billetera(long id, String saldo, String apartado, String negocio) {
        return Billetera.reconstituir(id, new UsuarioId(id), new Celular("98765432" + id), negocio,
                Dinero.de(saldo), Dinero.de(apartado), EstadoBilletera.ACTIVA, null, AHORA, 0L);
    }

    private static AcumuladoMensual acumulado(String recargas, String pagos) {
        return new AcumuladoMensual(Dinero.de(recargas), Dinero.de(pagos));
    }

    private static ErrorBilletera error(Result<Void, ErrorBilletera> r) {
        assertThat(r).isInstanceOf(Result.Err.class);
        return ((Result.Err<Void, ErrorBilletera>) r).error();
    }

    @Test
    void losTopesSeCalculanConLaUit() {
        assertThat(TOPES.porOperacion()).isEqualTo(Dinero.de("3300"));
        assertThat(TOPES.saldoMaximo()).isEqualTo(Dinero.de("10450"));
        assertThat(TOPES.recargasMes()).isEqualTo(Dinero.de("10450"));
        assertThat(TOPES.pagosMes()).isEqualTo(Dinero.de("15400"));
    }

    @Test
    void rn01AceptaJustoElTopeYRechazaUnCentimoMas() {
        var b = billetera(1, "0", "0", null);
        assertThat(TopesSBS.validarRecarga(b, Dinero.de("3300"), AcumuladoMensual.VACIO, TOPES)).isInstanceOf(Result.Ok.class);
        assertThat(error(TopesSBS.validarRecarga(b, Dinero.de("3300.01"), AcumuladoMensual.VACIO, TOPES)))
                .isInstanceOf(ErrorBilletera.TopePorOperacion.class);
    }

    @Test
    void rn02ElSaldoNoPuedeSuperarElMaximo() {
        var casiLlena = billetera(1, "10000", "0", null);
        assertThat(error(TopesSBS.validarRecarga(casiLlena, Dinero.de("450.01"), AcumuladoMensual.VACIO, TOPES)))
                .isEqualTo(new ErrorBilletera.TopeSaldoPropio(Dinero.de("450")));

        var origen = billetera(2, "1000", "0", null);
        assertThat(error(TopesSBS.validarEnvio(origen, casiLlena, Dinero.de("500"), AcumuladoMensual.VACIO, TOPES)))
                .isInstanceOf(ErrorBilletera.DestinoNoPuedeRecibir.class);
    }

    @Test
    void rn03TopeMensualDeRecargas() {
        var b = billetera(1, "0", "0", null);
        assertThat(error(TopesSBS.validarRecarga(b, Dinero.de("500"), acumulado("10000", "0"), TOPES)))
                .isEqualTo(new ErrorBilletera.TopeRecargasMes(Dinero.de("450")));
    }

    @Test
    void rn04TopeMensualDePagosYTransferencias() {
        var origen = billetera(1, "3000", "0", null);
        var destino = billetera(2, "0", "0", "Bodega");
        assertThat(error(TopesSBS.validarEnvio(origen, destino, Dinero.de("200"), acumulado("0", "15300"), TOPES)))
                .isEqualTo(new ErrorBilletera.TopePagosMes(Dinero.de("100")));
        assertThat(TopesSBS.validarEnvio(origen, destino, Dinero.de("100"), acumulado("0", "15300"), TOPES))
                .isInstanceOf(Result.Ok.class);
    }

    @Test
    void loApartadoParaLaCuotaNoSePuedeGastar() {
        var origen = billetera(1, "100", "40", null);
        var destino = billetera(2, "0", "0", null);
        assertThat(origen.saldoDisponible()).isEqualTo(Dinero.de("60"));
        assertThat(error(TopesSBS.validarEnvio(origen, destino, Dinero.de("60.01"), AcumuladoMensual.VACIO, TOPES)))
                .isInstanceOf(ErrorBilletera.SaldoInsuficiente.class);
        assertThat(error(origen.debitar(Dinero.de("60.01")))).isInstanceOf(ErrorBilletera.SaldoInsuficiente.class);
        assertThat(origen.debitar(Dinero.de("60"))).isInstanceOf(Result.Ok.class);
        assertThat(origen.getSaldo()).isEqualTo(Dinero.de("40"));
    }

    @Test
    void noSePuedeEnviarASiMismoNiDesdeUnaBilleteraBloqueada() {
        var b = billetera(1, "100", "0", null);
        assertThat(error(TopesSBS.validarEnvio(b, b, Dinero.de("10"), AcumuladoMensual.VACIO, TOPES)))
                .isInstanceOf(ErrorBilletera.OperacionConSiMismo.class);

        var bloqueada = Billetera.reconstituir(3L, new UsuarioId(3L), new Celular("987654323"), null,
                Dinero.de("100"), Dinero.CERO, EstadoBilletera.BLOQUEADA, "Cumplimiento", AHORA, 0L);
        assertThat(error(TopesSBS.validarEnvio(bloqueada, b, Dinero.de("10"), AcumuladoMensual.VACIO, TOPES)))
                .isInstanceOf(ErrorBilletera.BilleteraBloqueada.class);
    }

    @Test
    void laTransferenciaGeneraDosMovimientosConElSaldoPosterior() {
        var origen = billetera(1, "100", "0", null);
        var destino = billetera(2, "50", "0", null);
        origen.debitar(Dinero.de("30"));
        destino.acreditar(Dinero.de("30"), TOPES.saldoMaximo(), true);

        var t = Transaccion.entreBilleteras(TipoTransaccion.TRANSFERENCIA, origen, destino, Dinero.de("30"),
                "ref-1", null, AHORA);
        assertThat(t.movimientos()).containsExactly(
                new Transaccion.Movimiento(1L, new BigDecimal("-30.00"), Dinero.de("70")),
                new Transaccion.Movimiento(2L, new BigDecimal("30.00"), Dinero.de("80")));
    }

    @Test
    void elCelularTieneNueveDigitosYEmpiezaConNueve() {
        assertThat(Celular.de("987 654 321")).contains(new Celular("987654321"));
        assertThat(Celular.de("887654321")).isEmpty();
        assertThat(Celular.de("98765432")).isEmpty();
        assertThat(new Celular("987654321").enmascarado()).isEqualTo("987 *** 321");
    }
}
