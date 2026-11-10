package com.projects.kapikua_api.identidad.application.port.out;

/** El celular pertenece a la billetera: lo responde Billetera. */
public interface CelularDisponible {

    boolean estaLibre(String celular);
}
