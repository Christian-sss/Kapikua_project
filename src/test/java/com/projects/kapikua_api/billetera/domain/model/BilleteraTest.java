package com.projects.kapikua_api.billetera.domain.model;

import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.billetera.domain.event.EstadoCambiado;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class BilleteraTest {
    static final OffsetDateTime AHORA = OffsetDateTime.of(2026, 11, 9, 10, 0, 0, 0, ZoneOffset.ofHours(-5));
    static final Dinero MAXIMO = Dinero.de("2000");
    static final Celular CELULAR = new Celular("987654321");

    static Billetera conSaldo(String saldo, String apartado, String estado) {
        return Billetera.reconstituir(1L, new UsuarioId(10L), CELULAR, "Bodega Ana", Dinero.de(saldo),
                Dinero.de(apartado), estado, null, AHORA, 0L);
    }

    static Billetera activa(String saldo) {
        return conSaldo(saldo, "0", "ACTIVA");
    }

    static ErrorBilletera error(Result<?, ErrorBilletera> r) {
        assertThat(r.esOk()).as("se esperaba un error").isFalse();
        return ((Result.Err<?, ErrorBilletera>) r).error();
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
        var b = conSaldo("100", "80", "ACTIVA");
        assertThat(b.saldoDisponible()).isEqualTo(Dinero.de("20"));
        assertThat(error(b.debitar(Dinero.de("30")))).isInstanceOf(ErrorBilletera.SaldoInsuficiente.class);
        assertThat(b.getSaldo()).isEqualTo(Dinero.de("100"));
    }

    @Test
    void noSeDebitaCeroNiDesdeUnaBilleteraBloqueada() {
        assertThat(error(activa("100").debitar(Dinero.CERO))).isInstanceOf(ErrorBilletera.MontoInvalido.class);
        var bloqueada = conSaldo("100", "0", "BLOQUEADA");
        assertThat(error(bloqueada.debitar(Dinero.de("10")))).isInstanceOf(ErrorBilletera.BilleteraBloqueada.class);
        assertThat(error(bloqueada.debitar(Dinero.CERO))).isInstanceOf(ErrorBilletera.BilleteraBloqueada.class);
        assertThat(bloqueada.getSaldo()).isEqualTo(Dinero.de("100"));
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
        assertThat(error(conSaldo("0", "0", "BLOQUEADA").acreditar(Dinero.de("10"), MAXIMO, true)))
                .isInstanceOf(ErrorBilletera.DestinatarioNoEncontrado.class);
    }

    @Test
    void unaBilleteraPropiaBloqueadaNoRecibeNiSeAcreditaCero() {
        assertThat(error(conSaldo("0", "0", "BLOQUEADA").acreditar(Dinero.de("10"), MAXIMO, false)))
                .isInstanceOf(ErrorBilletera.BilleteraBloqueada.class);
        assertThat(error(activa("0").acreditar(Dinero.CERO, MAXIMO, false))).isInstanceOf(ErrorBilletera.MontoInvalido.class);
    }

    @Test
    void apartaLoPedidoSiAlcanza() {
        var b = activa("100");
        assertThat(b.apartar(Dinero.de("40"))).isEqualTo(Result.ok(Dinero.de("40")));
        assertThat(b.getSaldoApartado()).isEqualTo(Dinero.de("40"));
        assertThat(b.getSaldo()).isEqualTo(Dinero.de("100"));
    }

    @Test
    void siNoAlcanzaApartaSoloLoDisponible() {
        var b = conSaldo("100", "70", "ACTIVA");
        assertThat(b.apartar(Dinero.de("50"))).isEqualTo(Result.ok(Dinero.de("30")));
        assertThat(b.saldoDisponible()).isEqualTo(Dinero.CERO);
    }

    @Test
    void liberarDevuelveLoApartadoSinQuedarNegativo() {
        var b = conSaldo("100", "40", "ACTIVA");
        assertThat(b.liberarApartado(Dinero.de("15"))).isEqualTo(Result.ok(Dinero.de("15")));
        assertThat(b.getSaldoApartado()).isEqualTo(Dinero.de("25"));
        assertThat(b.liberarApartado(Dinero.de("99"))).isEqualTo(Result.ok(Dinero.de("25")));
        assertThat(b.getSaldoApartado()).isEqualTo(Dinero.CERO);
        assertThat(b.getSaldo()).isEqualTo(Dinero.de("100"));
    }

    @Test
    void consumirApartadoSacaDelSaldoYDelApartado() {
        var b = conSaldo("100", "40", "ACTIVA");
        assertThat(b.consumirApartado(Dinero.de("40"))).isEqualTo(Result.ok(Dinero.de("40")));
        assertThat(b.getSaldo()).isEqualTo(Dinero.de("60"));
        assertThat(b.getSaldoApartado()).isEqualTo(Dinero.CERO);
        assertThat(error(b.consumirApartado(Dinero.de("1"))))
                .isInstanceOf(ErrorBilletera.SaldoApartadoInsuficiente.class);
    }

    @Test
    void puedeRecibirEsLoQueFaltaParaElMaximo() {
        assertThat(activa("1200").puedeRecibir(MAXIMO)).isEqualTo(Dinero.de("800"));
    }

    @Test
    void bloquearGuardaElMotivoRecortadoA150Caracteres() {
        var b = activa("100");
        var bloqueada = b.bloquear("  " + "x".repeat(200) + "  ", AHORA);
        assertThat(bloqueada).isEqualTo(Result.ok(new EstadoCambiado(1L, "ACTIVA", "BLOQUEADA", "x".repeat(150), AHORA)));
        assertThat(b.getEstado()).isEqualTo("BLOQUEADA");
        assertThat(b.getMotivoBloqueo()).hasSize(150);
    }

    @Test
    void unSegundoBloqueoConservaElPrimerMotivo() {
        var b = activa("100");
        assertThat(b.bloquear("Alerta 7", AHORA).esOk()).isTrue();
        assertThat(error(b.bloquear(" ", AHORA))).isInstanceOf(ErrorBilletera.MotivoEstadoInvalido.class);
        assertThat(error(b.bloquear("Alerta 9", AHORA))).isInstanceOf(ErrorBilletera.BilleteraYaBloqueada.class);
        assertThat(b.getMotivoBloqueo()).isEqualTo("Alerta 7");
    }

    @Test
    void elBloqueoExigeUnMotivo() {
        assertThat(error(activa("100").bloquear(" ", AHORA))).isInstanceOf(ErrorBilletera.MotivoEstadoInvalido.class);
        assertThat(error(activa("100").bloquear(null, AHORA))).isInstanceOf(ErrorBilletera.MotivoEstadoInvalido.class);
    }

    @Test
    void retenidaSoloRecibeDineroYSuValidacionNoMueveSaldo() {
        var b = conSaldo("100", "40", "RETENIDA");
        var monto = Dinero.de("10");

        assertThat(b.validarAcreditacion(monto, MAXIMO, true).esOk()).isTrue();
        assertThat(b.getSaldo()).isEqualTo(Dinero.de("100"));
        assertThat(b.acreditar(monto, MAXIMO, true).esOk()).isTrue();
        assertThat(error(b.debitar(monto))).isInstanceOf(ErrorBilletera.BilleteraRetenida.class);
        assertThat(error(b.apartar(monto))).isInstanceOf(ErrorBilletera.BilleteraRetenida.class);
        assertThat(error(b.liberarApartado(monto))).isInstanceOf(ErrorBilletera.BilleteraRetenida.class);
        assertThat(error(b.consumirApartado(monto))).isInstanceOf(ErrorBilletera.BilleteraRetenida.class);
        assertThat(b.getSaldo()).isEqualTo(Dinero.de("110"));
        assertThat(b.getSaldoApartado()).isEqualTo(Dinero.de("40"));
    }

    @Test
    void bloqueadaRechazaTambienOperacionesConSaldoApartado() {
        var b = conSaldo("100", "40", "BLOQUEADA");
        var monto = Dinero.de("10");

        assertThat(error(b.apartar(monto))).isInstanceOf(ErrorBilletera.BilleteraBloqueada.class);
        assertThat(error(b.liberarApartado(monto))).isInstanceOf(ErrorBilletera.BilleteraBloqueada.class);
        assertThat(error(b.consumirApartado(monto))).isInstanceOf(ErrorBilletera.BilleteraBloqueada.class);
        assertThat(b.getSaldo()).isEqualTo(Dinero.de("100"));
        assertThat(b.getSaldoApartado()).isEqualTo(Dinero.de("40"));
    }

    @Test
    void elEstadoDeterminaTransicionesYElEventoConservaSuOrigen() {
        var b = activa("100");
        assertThat(error(b.reactivar(AHORA))).isInstanceOf(ErrorBilletera.BilleteraYaActiva.class);
        assertThat(b.retener("  Revisión  ", AHORA))
                .isEqualTo(Result.ok(new EstadoCambiado(1L, "ACTIVA", "RETENIDA", "Revisión", AHORA)));
        assertThat(error(b.retener("Otro motivo", AHORA))).isInstanceOf(ErrorBilletera.BilleteraYaRetenida.class);
        assertThat(b.bloquear("Alerta", AHORA))
                .isEqualTo(Result.ok(new EstadoCambiado(1L, "RETENIDA", "BLOQUEADA", "Alerta", AHORA)));
        assertThat(b.estaBloqueada()).isTrue();
        assertThat(error(b.retener("Revisión", AHORA))).isInstanceOf(ErrorBilletera.BilleteraBloqueada.class);
        assertThat(b.reactivar(AHORA))
                .isEqualTo(Result.ok(new EstadoCambiado(1L, "BLOQUEADA", "ACTIVA", null, AHORA)));
        assertThat(b.estaActiva()).isTrue();
        assertThat(b.getMotivoBloqueo()).isNull();
    }

    @Test
    void retenidaPuedeReactivarseDirectamente() {
        var b = conSaldo("100", "40", "RETENIDA");
        assertThat(b.reactivar(AHORA))
                .isEqualTo(Result.ok(new EstadoCambiado(1L, "RETENIDA", "ACTIVA", null, AHORA)));
        assertThat(b.debitar(Dinero.de("20")).esOk()).isTrue();
    }
}
