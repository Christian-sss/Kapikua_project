package com.projects.kapikua_api.administracion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public record Grafico(String titulo, List<Barra> barras) {
    public record Barra(String etiqueta, String valor, int porcentaje) {}

    public static Grafico de(String titulo, Map<String, BigDecimal> datos, Function<BigDecimal, String> formato) {
        var maximo = datos.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        var barras = datos.entrySet().stream().map(e -> new Barra(e.getKey(), formato.apply(e.getValue()),
                maximo.signum() == 0 ? 0 : e.getValue().multiply(BigDecimal.valueOf(100))
                        .divide(maximo, 0, java.math.RoundingMode.HALF_UP).intValue())).toList();
        return new Grafico(titulo, barras);
    }

    /** Cada barra muestra su parte del total en porcentaje (la suma da 100 %). */
    public static Grafico porcentajes(String titulo, Map<String, BigDecimal> datos) {
        var total = datos.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        var barras = datos.entrySet().stream().map(e -> {
            int pct = total.signum() == 0 ? 0 : e.getValue().multiply(BigDecimal.valueOf(100))
                    .divide(total, 0, java.math.RoundingMode.HALF_UP).intValue();
            return new Barra(e.getKey(), pct + " %", pct);
        }).toList();
        return new Grafico(titulo, barras);
    }

    /** Coordenadas (0–100) de cada punto para el gráfico de línea: x al centro de su columna, y según el porcentaje. */
    public String puntos() {
        var n = barras.size();
        var sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(' ');
            sb.append(String.format(java.util.Locale.ROOT, "%.2f,%d", (i + 0.5) * 100.0 / n, 100 - barras.get(i).porcentaje()));
        }
        return sb.toString();
    }

    public boolean vacio() {
        return barras.stream().allMatch(b -> b.porcentaje() == 0);
    }
}
