package com.projects.kapikua_api.billetera.domain.model;

import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Result;

import java.util.Map;

abstract sealed class EstadoBilletera permits EstadoActivo, EstadoRetenido, EstadoBloqueado {
    static final EstadoBilletera ACTIVA = new EstadoActivo();
    static final EstadoBilletera RETENIDA = new EstadoRetenido();
    static final EstadoBilletera BLOQUEADA = new EstadoBloqueado();

    private static final Map<String, EstadoBilletera> POR_NOMBRE = Map.of(
            ACTIVA.nombre(), ACTIVA, RETENIDA.nombre(), RETENIDA, BLOQUEADA.nombre(), BLOQUEADA);

    private final String nombre;

    EstadoBilletera(String nombre) {
        this.nombre = nombre;
    }

    static EstadoBilletera desdeNombre(String nombre) {
        var estado = nombre == null ? null : POR_NOMBRE.get(nombre);
        if (estado == null) throw new IllegalArgumentException("Estado de billetera desconocido: " + nombre);
        return estado;
    }

    final String nombre() { return nombre; }

    boolean estaActiva() { return false; }

    boolean estaBloqueada() { return false; }

    abstract Result<Void, ErrorBilletera> validarRecepcion(boolean esDestinoAjeno);

    abstract Result<Void, ErrorBilletera> validarSalida();

    abstract Result<EstadoBilletera, ErrorBilletera> bloquear();

    abstract Result<EstadoBilletera, ErrorBilletera> retener();

    abstract Result<EstadoBilletera, ErrorBilletera> reactivar();
}
