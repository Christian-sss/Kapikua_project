package com.projects.kapikua_api.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Dinero(BigDecimal valor) implements Comparable<Dinero> {
    public static final Dinero CERO = new Dinero(BigDecimal.ZERO);

    public Dinero {
        if (valor == null) throw new IllegalArgumentException("El monto no puede ser nulo");
        if (valor.signum() < 0) throw new IllegalArgumentException("El monto no puede ser negativo: " + valor);
        valor = valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static Dinero de(String valor) {
        return new Dinero(new BigDecimal(valor));
    }

    public static Dinero de(BigDecimal valor) {
        return new Dinero(valor);
    }

    public Dinero sumar(Dinero otro) {
        return new Dinero(valor.add(otro.valor));
    }

    public Dinero restar(Dinero otro) {
        return new Dinero(valor.subtract(otro.valor));
    }

    public Dinero faltaPara(Dinero limite) {
        return menorQue(limite) ? limite.restar(this) : CERO;
    }

    public Dinero multiplicar(BigDecimal factor) {
        return new Dinero(valor.multiply(factor));
    }

    public boolean esCero() {
        return valor.signum() == 0;
    }

    public boolean mayorQue(Dinero otro) {
        return compareTo(otro) > 0;
    }

    public boolean menorQue(Dinero otro) {
        return compareTo(otro) < 0;
    }

    @Override
    public int compareTo(Dinero otro) {
        return valor.compareTo(otro.valor);
    }

    @Override
    public String toString() {
        return "S/ " + valor.toPlainString();
    }
}
