package com.projects.kapikua_api.credito.domain.service;

import com.projects.kapikua_api.credito.domain.model.*;

import com.projects.kapikua_api.shared.Dinero;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraFrancesaTest {

    private static final LocalDate DESEMBOLSO = LocalDate.of(2026, 11, 9);

    /** Informe de factibilidad, tabla 15: S/ 1 000, 12 cuotas semanales, TEA 90 %: cuota S/ 90,29 e interés S/ 83,50. */
    @Test
    void cronogramaDelEjemploDelInformeDeFactibilidad() {
        var c = CalculadoraFrancesa.cronograma(Dinero.de("1000"), new BigDecimal("90"), 12, DESEMBOLSO);

        assertThat(c.cuota()).isEqualTo(Dinero.de("90.29"));
        assertThat(c.cuotas()).hasSize(12);
        assertThat(c.cuotas().stream().map(CalculadoraFrancesa.CuotaProgramada::capital).reduce(Dinero.CERO, Dinero::sumar))
                .isEqualTo(Dinero.de("1000"));                              // el capital suma exacto el monto
        assertThat(c.interesTotal().valor()).isBetween(new BigDecimal("83.40"), new BigDecimal("83.60"));
        assertThat(c.cuotas().subList(0, 11)).allSatisfy(q -> assertThat(q.total()).isEqualTo(Dinero.de("90.29")));
        assertThat(c.cuotas().get(11).total().valor())                      // la última absorbe el redondeo
                .isBetween(new BigDecimal("90.20"), new BigDecimal("90.38"));
    }

    @Test
    void lasCuotasVencenCadaSieteDiasDesdeElDesembolso() {
        var c = CalculadoraFrancesa.cronograma(Dinero.de("800"), new BigDecimal("95"), 8, DESEMBOLSO);
        assertThat(c.cuotas().get(0).vencimiento()).isEqualTo(DESEMBOLSO.plusDays(7));
        assertThat(c.cuotas().get(7).vencimiento()).isEqualTo(DESEMBOLSO.plusDays(56));
        assertThat(c.cuotas().get(0).numero()).isEqualTo(1);
    }

    @Test
    void elInteresBajaYElCapitalSubeCadaSemana() {
        var c = CalculadoraFrancesa.cronograma(Dinero.de("2000"), new BigDecimal("70"), 24, DESEMBOLSO);
        for (int k = 1; k < 23; k++) {
            assertThat(c.cuotas().get(k).interes()).isLessThanOrEqualTo(c.cuotas().get(k - 1).interes());
            assertThat(c.cuotas().get(k).capital()).isGreaterThanOrEqualTo(c.cuotas().get(k - 1).capital());
        }
    }

    @Test
    void sinComisionesLaTceaEsLaTea() {
        assertThat(CalculadoraFrancesa.tcea(new BigDecimal("95"))).isEqualByComparingTo("95");
    }
}
