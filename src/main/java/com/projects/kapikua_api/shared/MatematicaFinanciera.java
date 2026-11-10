package com.projects.kapikua_api.shared;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class MatematicaFinanciera {
    private static final MathContext MC = new MathContext(24, RoundingMode.HALF_EVEN);
    private static final int DIAS_POR_ANIO = 360;
    private static final int DIAS_POR_SEMANA = 7;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private MatematicaFinanciera() {}

    public static BigDecimal tasaSemanal(BigDecimal teaPorcentaje) {
        if (teaPorcentaje.signum() < 0) throw new IllegalArgumentException("TEA negativa");
        var base = BigDecimal.ONE.add(teaPorcentaje.divide(CIEN, MC), MC);
        return raiz(base.pow(DIAS_POR_SEMANA, MC), DIAS_POR_ANIO).subtract(BigDecimal.ONE, MC);
    }

    public static BigDecimal tasaDiaria(BigDecimal tasaAnualPorcentaje) {
        if (tasaAnualPorcentaje.signum() < 0) throw new IllegalArgumentException("Tasa negativa");
        var base = BigDecimal.ONE.add(tasaAnualPorcentaje.divide(CIEN, MC), MC);
        return raiz(base, DIAS_POR_ANIO).subtract(BigDecimal.ONE, MC);
    }

    public static BigDecimal factorAnualidad(BigDecimal i, int n) {
        if (n <= 0) throw new IllegalArgumentException("Número de cuotas inválido: " + n);
        if (i.signum() == 0) return BigDecimal.valueOf(n);
        var descuento = BigDecimal.ONE.divide(BigDecimal.ONE.add(i, MC).pow(n, MC), MC);
        return BigDecimal.ONE.subtract(descuento, MC).divide(i, MC);
    }

    public static BigDecimal valorPresente(BigDecimal cuota, BigDecimal i, int n) {
        return cuota.multiply(factorAnualidad(i, n), MC);
    }

    public static Dinero cuota(Dinero monto, BigDecimal i, int n) {
        return Dinero.de(monto.valor().divide(factorAnualidad(i, n), MC));
    }

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
