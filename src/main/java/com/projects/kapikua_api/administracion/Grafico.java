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

    public boolean vacio() {
        return barras.stream().allMatch(b -> b.porcentaje() == 0);
    }
}
