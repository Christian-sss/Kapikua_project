package com.projects.kapikua_api.credito.model;

import com.projects.kapikua_api.credito.error.ErrorCredito;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

abstract sealed class EstadoCuota permits CuotaPendiente, CuotaVencida, CuotaPagada, CuotaAnulada {
    static final EstadoCuota PENDIENTE = new CuotaPendiente();
    static final EstadoCuota VENCIDA = new CuotaVencida();
    static final EstadoCuota PAGADA = new CuotaPagada();
    static final EstadoCuota ANULADA = new CuotaAnulada();

    private static final Map<String, EstadoCuota> POR_NOMBRE = Map.of(
            PENDIENTE.nombre(), PENDIENTE, VENCIDA.nombre(), VENCIDA,
            PAGADA.nombre(), PAGADA, ANULADA.nombre(), ANULADA);

    private final String nombre;

    EstadoCuota(String nombre) {
        this.nombre = nombre;
    }

    static EstadoCuota desdeNombre(String nombre) {
        var estado = POR_NOMBRE.get(nombre);
        if (estado == null) throw new IllegalArgumentException("Estado de cuota desconocido: " + nombre);
        return estado;
    }

    final String nombre() { return nombre; }

    final boolean porCobrar() { return esPendiente() || estaVencida(); }

    boolean esPendiente() { return false; }

    boolean estaVencida() { return false; }

    Result<Cuota.PagoImputado, ErrorCredito> imputar(Cuota cuota, Dinero monto, Dinero deApartado, LocalDate hoy) {
        return Result.err(errorEstado(cuota));
    }

    Result<CambioEstado, ErrorCredito> vencer(Cuota cuota, LocalDate hoy, OffsetDateTime ahora) {
        return Result.err(errorEstado(cuota));
    }

    Result<CambioEstado, ErrorCredito> anular(Cuota cuota, OffsetDateTime ahora) {
        return Result.err(errorEstado(cuota));
    }

    Result<Void, ErrorCredito> verificarReprogramable(Cuota cuota) {
        return Result.err(errorEstado(cuota));
    }

    final ErrorCredito errorEstado(Cuota cuota) {
        return new ErrorCredito.CuotaEstadoInvalido(cuota.getNumero(), nombre);
    }
}
