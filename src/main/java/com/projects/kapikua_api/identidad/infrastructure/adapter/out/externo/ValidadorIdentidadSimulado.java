package com.projects.kapikua_api.identidad.infrastructure.adapter.out.externo;

import com.projects.kapikua_api.identidad.application.port.out.ValidadorIdentidad;
import com.projects.kapikua_api.identidad.domain.model.IdentidadVerificada;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
class ValidadorIdentidadSimulado implements ValidadorIdentidad {
    @Override
    public Optional<IdentidadVerificada> validar(String dni, LocalDate fechaNacimiento) {
        if (dni == null || dni.length() != 8 || dni.startsWith("000") || fechaNacimiento == null) {
            return Optional.empty();
        }
        return Optional.of(new IdentidadVerificada("Cliente", "Prueba " + dni.substring(4)));
    }

    @Override
    public boolean figuraEnListas(String dni) {
        return dni != null && dni.endsWith("666");
    }
}
