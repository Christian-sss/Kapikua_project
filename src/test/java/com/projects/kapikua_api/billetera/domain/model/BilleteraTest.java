package com.projects.kapikua_api.billetera.domain.model;

import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BilleteraTest {
    static final OffsetDateTime AHORA = OffsetDateTime.of(2026, 11, 9, 10, 0, 0, 0, ZoneOffset.ofHours(-5));
    static final Dinero MAXIMO = Dinero.de("2000");
    static final Celular CELULAR = new Celular("987654321");

    static Billetera conSaldo(String saldo, String apartado, EstadoBilletera estado) {
        return Billetera.reconstituir(1L, new UsuarioId(10L), CELULAR, "Bodega Ana", Dinero.de(saldo),
                Dinero.de(apartado), estado, null, AHORA, 0L);
    }

    static Billetera activa(String saldo) {
        return conSaldo(saldo, "0", EstadoBilletera.ACTIVA);
    }

    static ErrorBilletera error(Result<Void, ErrorBilletera> r) {
        assertThat(r.esOk()).as("se esperaba un error").isFalse();
        return ((Result.Err<Void, ErrorBilletera>) r).error();
    }

    @Test
    void seAbreActivaConSaldoCeroYSinNegocioSiElNombreEstaEnBlanco() {
        var b = Billetera.abrir(new UsuarioId(10L), CELULAR, "   ", AHORA);
        assertThat(b.estaActiva()).isTrue();
        assertThat(b.getSaldo()).isEqualTo(Dinero.CERO);
        assertThat(b.getSaldoApartado()).isEqualTo(Dinero.CERO);
        assertThat(b.esNegocio()).isFalse();
        assertThat(b.getId()).isNull();
        assertThat(b.getFechaApertura()).isEqualTo(AHORA);
    }

    @Test
    void elNombreDelNegocioSeGuardaSinEspacios() {
        var b = Billetera.abrir(new UsuarioId(10L), CELULAR, "  Bodega Ana ", AHORA);
        assertThat(b.esNegocio()).isTrue();
        assertThat(b.getNombreNegocio()).isEqualTo("Bodega Ana");
    }

    @Test
    void debitarRestaDelSaldo() {
        var b = activa("100");
        assertThat(b.debitar(Dinero.de("30")).esOk()).isTrue();
        assertThat(b.getSaldo()).isEqualTo(Dinero.de("70"));
    }

    @Test
    void loApartadoNoSePuedeGastar() {
        var b = conSaldo("100", "80", EstadoBilletera.ACTIVA);
        assertThat(b.saldoDisponible()).isEqualTo(Dinero.de("20"));
        assertThat(error(b.debitar(Dinero.de("30")))).isInstanceOf(ErrorBilletera.SaldoInsuficiente.class);
        assertThat(b.getSaldo()).isEqualTo(Dinero.de("100"));
    }

    @Test
    void noSeDebitaCeroNiDesdeUnaBilleteraBloqueada() {
        assertThat(error(activa("100").debitar(Dinero.CERO))).isInstanceOf(ErrorBilletera.MontoInvalido.class);
        assertThat(error(conSaldo("100", "0", EstadoBilletera.BLOQUEADA).debitar(Dinero.de("10"))))
                .isInstanceOf(ErrorBilletera.BilleteraBloqueada.class);
    }

    @Test
    void acreditarSumaHastaElSaldoMaximoExacto() {
        var b = activa("1500");
        assertThat(b.acreditar(Dinero.de("500"), MAXIMO, false).esOk()).isTrue();
        assertThat(b.getSaldo()).isEqualTo(MAXIMO);
    }

    @Test
    void siElSaldoPropioSuperariaElMaximoDiceCuantoPuedeRecibir() {
        var b = activa("1500");
        var e = error(b.acreditar(Dinero.de("600"), MAXIMO, false));
        assertThat(e).isEqualTo(new ErrorBilletera.TopeSaldoPropio(Dinero.de("500")));
        assertThat(b.getSaldo()).isEqualTo(Dinero.de("1500"));
    }

    @Test
    void conUnDestinoAjenoNoRevelaSuSaldoNiSuEstado() {
        assertThat(error(activa("1500").acreditar(Dinero.de("600"), MAXIMO, true)))
                .isInstanceOf(ErrorBilletera.DestinoNoPuedeRecibir.class);
        assertThat(error(conSaldo("0", "0", EstadoBilletera.BLOQUEADA).acreditar(Dinero.de("10"), MAXIMO, true)))
                .isInstanceOf(ErrorBilletera.DestinatarioNoEncontrado.class);
    }

    @Test
    void unaBilleteraPropiaBloqueadaNoRecibeNiSeAcreditaCero() {
        assertThat(error(conSaldo("0", "0", EstadoBilletera.BLOQUEADA).acreditar(Dinero.de("10"), MAXIMO, false)))
                .isInstanceOf(ErrorBilletera.BilleteraBloqueada.class);
        assertThat(error(activa("0").acreditar(Dinero.CERO, MAXIMO, false))).isInstanceOf(ErrorBilletera.MontoInvalido.class);
    }

    @Test
    void apartaLoPedidoSiAlcanza() {
        var b = activa("100");
        assertThat(b.apartar(Dinero.de("40"))).isEqualTo(Dinero.de("40"));
        assertThat(b.getSaldoApartado()).isEqualTo(Dinero.de("40"));
        assertThat(b.getSaldo()).isEqualTo(Dinero.de("100"));
    }

    @Test
    void siNoAlcanzaApartaSoloLoDisponible() {
        var b = conSaldo("100", "70", EstadoBilletera.ACTIVA);
        assertThat(b.apartar(Dinero.de("50"))).isEqualTo(Dinero.de("30"));
        assertThat(b.saldoDisponible()).isEqualTo(Dinero.CERO);
    }

    @Test
    void liberarDevuelveLoApartadoSinQuedarNegativo() {
        var b = conSaldo("100", "40", EstadoBilletera.ACTIVA);
        b.liberarApartado(Dinero.de("15"));
        assertThat(b.getSaldoApartado()).isEqualTo(Dinero.de("25"));
        b.liberarApartado(Dinero.de("99"));
        assertThat(b.getSaldoApartado()).isEqualTo(Dinero.CERO);
        assertThat(b.getSaldo()).isEqualTo(Dinero.de("100"));
    }

    @Test
    void consumirApartadoSacaDelSaldoYDelApartado() {
        var b = conSaldo("100", "40", EstadoBilletera.ACTIVA);
        b.consumirApartado(Dinero.de("40"));
        assertThat(b.getSaldo()).isEqualTo(Dinero.de("60"));
        assertThat(b.getSaldoApartado()).isEqualTo(Dinero.CERO);
        assertThatThrownBy(() -> b.consumirApartado(Dinero.de("1"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void puedeRecibirEsLoQueFaltaParaElMaximo() {
        assertThat(activa("1200").puedeRecibir(MAXIMO)).isEqualTo(Dinero.de("800"));
    }

    @Test
    void bloquearGuardaElMotivoRecortadoA150Caracteres() {
        var b = activa("100");
        b.bloquear("  " + "x".repeat(200) + "  ");
        assertThat(b.getEstado()).isEqualTo(EstadoBilletera.BLOQUEADA);
        assertThat(b.getMotivoBloqueo()).hasSize(150);
    }

    @Test
    void unSegundoBloqueoConservaElPrimerMotivo() {
        var b = activa("100");
        b.bloquear("Alerta 7");
        b.bloquear("Alerta 9");
        assertThat(b.getMotivoBloqueo()).isEqualTo("Alerta 7");
    }

    @Test
    void elBloqueoExigeUnMotivo() {
        assertThatThrownBy(() -> activa("100").bloquear(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> activa("100").bloquear(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
