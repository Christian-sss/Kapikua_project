package com.projects.kapikua_api.credito.domain.service;

import com.projects.kapikua_api.credito.domain.model.*;

import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.MatematicaFinanciera;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de dominio: cuota y cronograma semanal por sistema francés (RF-CR-01). Usa la misma tasa semanal
 * que Evaluación (MatematicaFinanciera, año de 360 días). Cada cuota vence 7 días después de la anterior;
 * la primera, 7 días después del desembolso. La última absorbe el residuo del redondeo para que el capital
 * pagado sea exactamente el monto prestado.
 */
public final class CalculadoraFrancesa {

    private CalculadoraFrancesa() {}

    public record CuotaProgramada(int numero, LocalDate vencimiento, Dinero capital, Dinero interes) {
        public Dinero total() { return capital.sumar(interes); }
    }

    public record Cronograma(Dinero cuota, List<CuotaProgramada> cuotas) {
        public Dinero interesTotal() {
            return cuotas.stream().map(CuotaProgramada::interes).reduce(Dinero.CERO, Dinero::sumar);
        }

        public Dinero totalAPagar() {
            return cuotas.stream().map(CuotaProgramada::total).reduce(Dinero.CERO, Dinero::sumar);
        }
    }

    /** Capital e interés de un periodo. */
    public record Tramo(Dinero capital, Dinero interes) {}

    public static Cronograma cronograma(Dinero monto, BigDecimal teaPorcentaje, int numeroCuotas, LocalDate desembolso) {
        var i = MatematicaFinanciera.tasaSemanal(teaPorcentaje);
        var cuota = MatematicaFinanciera.cuota(monto, i, numeroCuotas);
        var tramos = amortizar(monto, i, cuota, numeroCuotas);
        var cuotas = new ArrayList<CuotaProgramada>(numeroCuotas);
        for (int n = 1; n <= numeroCuotas; n++) {
            var t = tramos.get(n - 1);
            cuotas.add(new CuotaProgramada(n, desembolso.plusWeeks(n), t.capital(), t.interes()));
        }
        return new Cronograma(cuota, List.copyOf(cuotas));
    }

    /**
     * Reparte {@code saldo} en {@code n} periodos con cuota fija: interés = saldo × i, capital = cuota − interés.
     * La última cuota paga todo el saldo que quede.
     */
    public static List<Tramo> amortizar(Dinero saldo, BigDecimal i, Dinero cuotaFija, int n) {
        var restante = saldo.valor();
        var tramos = new ArrayList<Tramo>(n);
        for (int k = 1; k <= n; k++) {
            var interes = restante.multiply(i).setScale(2, RoundingMode.HALF_EVEN);
            var capital = k == n ? restante : cuotaFija.valor().subtract(interes);
            if (capital.signum() < 0) capital = BigDecimal.ZERO;
            if (capital.compareTo(restante) > 0) capital = restante;
            tramos.add(new Tramo(Dinero.de(capital), Dinero.de(interes)));
            restante = restante.subtract(capital);
        }
        return List.copyOf(tramos);
    }

    /** RN-27, reducir plazo: el menor número de cuotas (hasta {@code maximo}) con el que la cuota fija paga el saldo. */
    public static int plazoPara(Dinero saldo, BigDecimal i, Dinero cuotaFija, int maximo) {
        for (int k = 1; k < maximo; k++) {
            if (MatematicaFinanciera.valorPresente(cuotaFija.valor(), i, k).compareTo(saldo.valor()) >= 0) return k;
        }
        return maximo;
    }

    /** Sin comisiones, gastos ni seguros cobrados al cliente, la TCEA es igual a la TEA. */
    public static BigDecimal tcea(BigDecimal tea) {
        return tea;
    }
}
