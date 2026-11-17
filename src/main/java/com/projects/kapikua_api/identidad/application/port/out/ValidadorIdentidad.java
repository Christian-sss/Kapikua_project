package com.projects.kapikua_api.identidad.application.port.out;

import com.projects.kapikua_api.identidad.domain.model.IdentidadVerificada;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

public interface ValidadorIdentidad {
    Optional<IdentidadVerificada> validar(String dni, LocalDate fechaNacimiento);

    /**
     * Valida la identidad y que los nombres y apellidos escritos coincidan con los de RENIEC
     * (CU-01, flujo 5a: «los datos no coinciden»). Sin coincidencia, la identidad no se confirma.
     */
    default Optional<IdentidadVerificada> validar(String dni, LocalDate fechaNacimiento,
                                                  String nombres, String apellidos) {
        return validar(dni, fechaNacimiento)
                .filter(reniec -> mismoTexto(reniec.nombres(), nombres) && mismoTexto(reniec.apellidos(), apellidos));
    }

    boolean figuraEnListas(String dni);

    /** Compara sin distinguir mayúsculas, tildes ni espacios repetidos. */
    static boolean mismoTexto(String a, String b) {
        return a != null && b != null && normalizar(a).equals(normalizar(b));
    }

    static String normalizar(String texto) {
        var sinTildes = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
