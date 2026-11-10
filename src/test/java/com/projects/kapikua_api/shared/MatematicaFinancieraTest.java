package com.projects.kapikua_api.shared;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.MathContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class MatematicaFinancieraTest {
    @Test
    void laTasaSemanalCapitalizadaUnAnioDe360DiasDevuelveLaTea() {
        var i = MatematicaFinanciera.tasaSemanal(new BigDecimal("95"));
        var izquierda = BigDecimal.ONE.add(i).pow(360, MathContext.DECIMAL128);
        var derecha = new BigDecimal("1.95").pow(7, MathContext.DECIMAL128);
        assertThat(izquierda).isCloseTo(derecha, within(new BigDecimal("1E-10")));
    }

    @Test
    void cuotaDelEjemploDelInformeDeFactibilidad() {
        var i = MatematicaFinanciera.tasaSemanal(new BigDecimal("90"));
        var cuota = MatematicaFinanciera.cuota(Dinero.de("1000"), i, 12);
        assertThat(cuota).isEqualTo(Dinero.de("90.29"));
        assertThat(cuota.multiplicar(BigDecimal.valueOf(12)).restar(Dinero.de("1000")).valor())
                .isCloseTo(new BigDecimal("83.50"), within(new BigDecimal("0.05")));
    }

    @Test
    void capacidadDeAna() {
        var i = MatematicaFinanciera.tasaSemanal(new BigDecimal("95"));
        assertThat(MatematicaFinanciera.valorPresente(new BigDecimal("450"), i, 8))
                .isCloseTo(new BigDecimal("3400"), within(new BigDecimal("5")));
    }

    @Test
    void capacidadDePedro() {
        var i = MatematicaFinanciera.tasaSemanal(new BigDecimal("80"));
        assertThat(MatematicaFinanciera.valorPresente(new BigDecimal("90"), i, 16))
                .isCloseTo(new BigDecimal("1310"), within(new BigDecimal("5")));
    }

    @Test
    void sinInteresLaCuotaEsElMontoEntreLasCuotas() {
        assertThat(MatematicaFinanciera.cuota(Dinero.de("1200"), BigDecimal.ZERO, 12)).isEqualTo(Dinero.de("100"));
    }
}
