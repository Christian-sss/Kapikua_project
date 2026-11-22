package com.projects.kapikua_api.credito.model;

import com.projects.kapikua_api.credito.error.ErrorCredito;
import com.projects.kapikua_api.shared.Result;

import java.time.OffsetDateTime;
import java.util.Map;

sealed abstract class EstadoAval permits AvalPendiente, AvalAceptado, AvalRechazado, AvalActivado, AvalLiberado {

    static final EstadoAval PENDIENTE = new AvalPendiente();
    static final EstadoAval ACEPTADO = new AvalAceptado();
    static final EstadoAval RECHAZADO = new AvalRechazado();
    static final EstadoAval ACTIVADO = new AvalActivado();
    static final EstadoAval LIBERADO = new AvalLiberado();

    private static final Map<String, EstadoAval> POR_NOMBRE = Map.of(
            PENDIENTE.nombre(), PENDIENTE,
            ACEPTADO.nombre(), ACEPTADO,
            RECHAZADO.nombre(), RECHAZADO,
            ACTIVADO.nombre(), ACTIVADO,
            LIBERADO.nombre(), LIBERADO);

    private final String nombre;

    EstadoAval(String nombre) {
        this.nombre = nombre;
    }

    static EstadoAval desdeNombre(String nombre) {
        var estado = POR_NOMBRE.get(nombre);
        if (estado == null) throw new IllegalArgumentException("Estado de aval desconocido: " + nombre);
        return estado;
    }

    final String nombre() {
        return nombre;
    }

    boolean esPendiente() { return false; }

    boolean estaAceptado() { return false; }

    boolean estaActivado() { return false; }

    final boolean tieneGarantia() { return estaAceptado() || estaActivado(); }

    final boolean enCurso() { return esPendiente() || tieneGarantia(); }

    Result<CambioEstado, ErrorCredito> aceptar(Aval aval, int porcentajeRetencion, String dispositivo,
                                               OffsetDateTime ahora) {
        return rechazarOperacion();
    }

    Result<CambioEstado, ErrorCredito> rechazar(Aval aval, OffsetDateTime cuando) {
        return rechazarOperacion();
    }

    boolean avisar(Aval aval, OffsetDateTime ahora) { return false; }

    Result<CambioEstado, ErrorCredito> activar(Aval aval, OffsetDateTime ahora) {
        return rechazarOperacion();
    }

    Result<CambioEstado, ErrorCredito> liberar(Aval aval, OffsetDateTime ahora) {
        return rechazarOperacion();
    }

    private Result<CambioEstado, ErrorCredito> rechazarOperacion() {
        return Result.err(new ErrorCredito.AvalEstadoInvalido(nombre));
    }
}
