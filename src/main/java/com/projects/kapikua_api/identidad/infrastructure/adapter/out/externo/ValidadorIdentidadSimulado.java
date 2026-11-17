package com.projects.kapikua_api.identidad.infrastructure.adapter.out.externo;

import com.projects.kapikua_api.identidad.application.port.out.ValidadorIdentidad;
import com.projects.kapikua_api.identidad.domain.model.IdentidadVerificada;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

/**
 * RENIEC y listas restrictivas simulados (OD-02). Un DNI de 8 dígitos que no empieza con «000» se considera
 * válido; uno que termina en «666» figura en listas restrictivas.
 */
@Component
class ValidadorIdentidadSimulado implements ValidadorIdentidad {
    @Override
    public Optional<IdentidadVerificada> validar(String dni, LocalDate fechaNacimiento) {
        if (!dniValido(dni, fechaNacimiento)) return Optional.empty();
        return Optional.of(new IdentidadVerificada("Cliente", "Prueba " + dni.substring(4)));
    }

    /** La simulación no tiene un padrón de nombres: confirma los que el cliente escribió. */
    @Override
    public Optional<IdentidadVerificada> validar(String dni, LocalDate fechaNacimiento,
                                                 String nombres, String apellidos) {
        if (!dniValido(dni, fechaNacimiento) || vacio(nombres) || vacio(apellidos)) return Optional.empty();
        return Optional.of(new IdentidadVerificada(limpiar(nombres), limpiar(apellidos)));
    }

    @Override
    public boolean figuraEnListas(String dni) {
        return dni != null && dni.endsWith("666");
    }

    private static boolean dniValido(String dni, LocalDate fechaNacimiento) {
        return dni != null && dni.length() == 8 && !dni.startsWith("000") && fechaNacimiento != null;
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    private static String limpiar(String texto) {
        return texto.trim().replaceAll("\\s+", " ");
    }
}
