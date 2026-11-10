package com.projects.kapikua_api.shared;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Sistema francés con cuotas semanales (RN-12, RF-CR-01). Todo en BigDecimal (RNF-09), sin pasar por double.
 * La usan Evaluación (capacidad de pago) y Créditos (cronograma), para que ambos calculen igual.
 * <p>
 * Convención del año de 360 días, la del sistema financiero peruano y la del informe de factibilidad:
 * con ella, S/ 1 000 en 12 cuotas semanales al 90 % TEA dan S/ 90,29 y S/ 83,50 de interés (FD01, tabla 15).
 */
public final class MatematicaFinanciera {

    private static final MathContext MC = new MathContext(24, RoundingMode.HALF_EVEN);
    private static final int DIAS_POR_ANIO = 360;
    private static final int DIAS_POR_SEMANA = 7;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private MatematicaFinanciera() {}

    /** i = (1 + TEA)^(7/360) − 1. {@code teaPorcentaje} en porcentaje: 95 significa 95 %. */
    public static BigDecimal tasaSemanal(BigDecimal teaPorcentaje) {
        if (teaPorcentaje.signum() < 0) throw new IllegalArgumentException("TEA negativa");
        var base = BigDecimal.ONE.add(teaPorcentaje.divide(CIEN, MC), MC);
        return raiz(base.pow(DIAS_POR_SEMANA, MC), DIAS_POR_ANIO).subtract(BigDecimal.ONE, MC);
    }

    /** Tasa diaria equivalente, año de 360 días: (1 + tasa)^(1/360) − 1. Para el interés moratorio (RN-24). */
    public static BigDecimal tasaDiaria(BigDecimal tasaAnualPorcentaje) {
        if (tasaAnualPorcentaje.signum() < 0) throw new IllegalArgumentException("Tasa negativa");
        var base = BigDecimal.ONE.add(tasaAnualPorcentaje.divide(CIEN, MC), MC);
        return raiz(base, DIAS_POR_ANIO).subtract(BigDecimal.ONE, MC);
    }

    /** Factor de valor presente de n cuotas: (1 − (1 + i)^−n) / i. Con i = 0 es n. */
    public static BigDecimal factorAnualidad(BigDecimal i, int n) {
        if (n <= 0) throw new IllegalArgumentException("Número de cuotas inválido: " + n);
        if (i.signum() == 0) return BigDecimal.valueOf(n);
        var descuento = BigDecimal.ONE.divide(BigDecimal.ONE.add(i, MC).pow(n, MC), MC);
        return BigDecimal.ONE.subtract(descuento, MC).divide(i, MC);
    }

    /** Monto que se puede pagar con una cuota dada: cuota × factor (sin redondear). */
    public static BigDecimal valorPresente(BigDecimal cuota, BigDecimal i, int n) {
        return cuota.multiply(factorAnualidad(i, n), MC);
    }

    /** Cuota fija del sistema francés: monto / factor, redondeada a céntimos. */
    public static Dinero cuota(Dinero monto, BigDecimal i, int n) {
        return Dinero.de(monto.valor().divide(factorAnualidad(i, n), MC));
    }

    /** x tal que x^n = a, para a ≥ 1 (Newton: converge en pocas iteraciones desde x = a). */
    static BigDecimal raiz(BigDecimal a, int n) {
        if (a.compareTo(BigDecimal.ONE) == 0) return BigDecimal.ONE;
        var x = BigDecimal.ONE.add(a.subtract(BigDecimal.ONE, MC).divide(BigDecimal.valueOf(n), MC), MC);
        var tolerancia = new BigDecimal("1E-20");
        for (int k = 0; k < 100; k++) {
            var xn1 = x.pow(n - 1, MC);
            var siguiente = x.subtract(x.multiply(xn1, MC).subtract(a, MC)
                    .divide(BigDecimal.valueOf(n).multiply(xn1, MC), MC), MC);
            if (siguiente.subtract(x, MC).abs().compareTo(tolerancia) < 0) return siguiente;
            x = siguiente;
        }
        return x;
    }
}
