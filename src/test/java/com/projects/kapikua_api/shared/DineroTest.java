package com.projects.kapikua_api.shared;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DineroTest {

    @Test
    void usaDosDecimalesConRedondeoBancario() {
        assertThat(Dinero.de("10.125").valor()).isEqualByComparingTo("10.12");
        assertThat(Dinero.de("10.135").valor()).isEqualByComparingTo("10.14");
        assertThat(Dinero.de("5")).isEqualTo(Dinero.de("5.00"));
    }

    @Test
    void sumaRestaYComparaSinPerderCentimos() {
        var a = Dinero.de("0.10");
        var b = Dinero.de("0.20");
        assertThat(a.sumar(b)).isEqualTo(Dinero.de("0.30"));
        assertThat(Dinero.de("100").restar(Dinero.de("99.99"))).isEqualTo(Dinero.de("0.01"));
        assertThat(a.menorQue(b)).isTrue();
        assertThat(b.mayorQue(a)).isTrue();
    }

    @Test
    void faltaParaNuncaEsNegativo() {
        assertThat(Dinero.de("30").faltaPara(Dinero.de("100"))).isEqualTo(Dinero.de("70"));
        assertThat(Dinero.de("150").faltaPara(Dinero.de("100"))).isEqualTo(Dinero.CERO);
    }

    @Test
    void multiplicaConLaUit() {
        assertThat(Dinero.de("5500").multiplicar(new BigDecimal("0.6"))).isEqualTo(Dinero.de("3300"));
        assertThat(Dinero.de("5500").multiplicar(new BigDecimal("1.9"))).isEqualTo(Dinero.de("10450"));
        assertThat(Dinero.de("5500").multiplicar(new BigDecimal("2.8"))).isEqualTo(Dinero.de("15400"));
    }

    @Test
    void rechazaMontosNegativos() {
        assertThatThrownBy(() -> Dinero.de("-0.01")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Dinero.de("1").restar(Dinero.de("2"))).isInstanceOf(IllegalArgumentException.class);
    }
}
