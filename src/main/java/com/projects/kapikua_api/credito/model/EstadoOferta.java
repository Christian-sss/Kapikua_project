package com.projects.kapikua_api.credito.model;

import com.projects.kapikua_api.credito.error.ErrorCredito;
import com.projects.kapikua_api.shared.Result;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;

abstract sealed class EstadoOferta permits OfertaPendienteEfa, OfertaConfirmada, OfertaDenegada,
        OfertaAceptada, OfertaVencida {
    static final EstadoOferta PENDIENTE_EFA = new OfertaPendienteEfa();
    static final EstadoOferta CONFIRMADA = new OfertaConfirmada();
    static final EstadoOferta DENEGADA = new OfertaDenegada();
    static final EstadoOferta ACEPTADA = new OfertaAceptada();
    static final EstadoOferta VENCIDA = new OfertaVencida();

    private static final Map<String, EstadoOferta> POR_NOMBRE = Map.of(
            PENDIENTE_EFA.nombre(), PENDIENTE_EFA, CONFIRMADA.nombre(), CONFIRMADA,
            DENEGADA.nombre(), DENEGADA, ACEPTADA.nombre(), ACEPTADA, VENCIDA.nombre(), VENCIDA);

    private final String nombre;

    EstadoOferta(String nombre) {
        this.nombre = nombre;
    }

    static EstadoOferta desdeNombre(String nombre) {
        var estado = POR_NOMBRE.get(nombre);
        if (estado == null) throw new IllegalArgumentException("Estado de oferta desconocido: " + nombre);
        return estado;
    }

    final String nombre() { return nombre; }

    final boolean enCurso() { return esperaDecisionEfa() || estaConfirmada(); }

    boolean esperaDecisionEfa() { return false; }

    boolean estaConfirmada() { return false; }

    boolean estaAceptada() { return false; }

    void validarReconstitucion(Oferta oferta) {

    }

    Result<Void, ErrorCredito> verificarGestionAval() {
        return Result.err(errorEstado());
    }

    Result<CambioEstado, ErrorCredito> confirmar(Oferta oferta, String codigo, CondicionesCredito condiciones,
                                                   OffsetDateTime ahora) {
        return Result.err(errorEstado());
    }

    Result<CambioEstado, ErrorCredito> denegar(Oferta oferta, String codigo, String motivo, OffsetDateTime ahora) {
        return Result.err(errorEstado());
    }

    Result<Void, ErrorCredito> verificarAceptable(Oferta oferta, boolean requiereAval, OffsetDateTime ahora, ZoneId zona) {
        return Result.err(errorAlAceptar(oferta, ahora, zona));
    }

    Result<CambioEstado, ErrorCredito> aceptar(Oferta oferta, boolean requiereAval, OffsetDateTime ahora, ZoneId zona) {
        return Result.err(errorAlAceptar(oferta, ahora, zona));
    }

    Result<CambioEstado, ErrorCredito> vencer(Oferta oferta, OffsetDateTime ahora, OffsetDateTime finVigencia) {
        return Result.err(errorEstado());
    }

    ErrorCredito errorAlAceptar(Oferta oferta, OffsetDateTime ahora, ZoneId zona) {
        return errorEstado();
    }

    final ErrorCredito errorEstado() {
        return new ErrorCredito.OfertaEstadoInvalido(nombre);
    }
}
