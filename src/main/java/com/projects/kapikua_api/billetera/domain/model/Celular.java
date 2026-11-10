package com.projects.kapikua_api.billetera.domain.model;

import java.util.Optional;
import java.util.regex.Pattern;

public record Celular(String numero) {
    private static final Pattern FORMATO = Pattern.compile("^9[0-9]{8}$");

    public Celular {
        if (numero == null || !FORMATO.matcher(numero).matches()) {
            throw new IllegalArgumentException("Celular inválido");
        }
    }

    public static Optional<Celular> de(String numero) {
        var limpio = numero == null ? "" : numero.replaceAll("\\s", "");
        return FORMATO.matcher(limpio).matches() ? Optional.of(new Celular(limpio)) : Optional.empty();
    }

    public String enmascarado() {
        return numero.substring(0, 3) + " *** " + numero.substring(6);
    }
}
