package com.projects.kapikua_api.identidad.infrastructure.adapter.out.externo;

import com.projects.kapikua_api.identidad.application.port.out.ValidadorIdentidad;
import com.projects.kapikua_api.identidad.domain.model.IdentidadVerificada;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

/**
 * RENIEC y listas restrictivas simulados, con reglas fijas para poder probar cada caso:
 * <ul>
 *   <li>DNI que empieza con "000": RENIEC no lo encuentra (CU-01, 5a).</li>
 *   <li>DNI que termina en "666": figura en listas restrictivas (CU-01, 6a).</li>
 *   <li>Cualquier otro DNI: RENIEC lo confirma con la fecha de nacimiento ingresada.</li>
 * </ul>
 * La edad (5b) se prueba ingresando una fecha de nacimiento reciente.
 */
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
