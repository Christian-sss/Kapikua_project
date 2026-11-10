package com.projects.kapikua_api.billetera.domain.model;

import java.util.Optional;
import java.util.regex.Pattern;

/** Número de celular peruano: 9 dígitos que empiezan con 9 (chk_billetera_celular). */
public record Celular(String numero) {

    private static final Pattern FORMATO = Pattern.compile("^9[0-9]{8}$");

    public Celular {
        if (numero == null || !FORMATO.matcher(numero).matches()) {
            throw new IllegalArgumentException("Celular inválido");
        }
    }

    /** Para datos que vienen del usuario: vacío si el formato no es válido. */
    public static Optional<Celular> de(String numero) {
        var limpio = numero == null ? "" : numero.replaceAll("\\s", "");
        return FORMATO.matcher(limpio).matches() ? Optional.of(new Celular(limpio)) : Optional.empty();
    }

    /** 987 *** 321 */
    public String enmascarado() {
        return numero.substring(0, 3) + " *** " + numero.substring(6);
    }
}
