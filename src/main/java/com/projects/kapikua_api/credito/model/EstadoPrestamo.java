package com.projects.kapikua_api.credito.model;

import com.projects.kapikua_api.credito.error.ErrorCredito;
import com.projects.kapikua_api.shared.Result;

import java.time.OffsetDateTime;
import java.util.Map;

abstract sealed class EstadoPrestamo permits PrestamoVigente, PrestamoEnMora, PrestamoPagado {
    static final EstadoPrestamo VIGENTE = new PrestamoVigente();
    static final EstadoPrestamo EN_MORA = new PrestamoEnMora();
    static final EstadoPrestamo PAGADO = new PrestamoPagado();

    private static final Map<String, EstadoPrestamo> POR_NOMBRE = Map.of(
            VIGENTE.nombre(), VIGENTE, EN_MORA.nombre(), EN_MORA, PAGADO.nombre(), PAGADO);

    private final String nombre;

    EstadoPrestamo(String nombre) {
        this.nombre = nombre;
    }

    static EstadoPrestamo desdeNombre(String nombre) {
        var estado = POR_NOMBRE.get(nombre);
        if (estado == null) throw new IllegalArgumentException("Estado de préstamo desconocido: " + nombre);
        return estado;
    }

    final String nombre() { return nombre; }

    final boolean estaActivo() { return !estaPagado(); }

    boolean estaPagado() { return false; }

    Result<Void, ErrorCredito> verificarCobro() {
        return Result.err(new ErrorCredito.PrestamoYaPagado());
    }

    Result<Void, ErrorCredito> verificarAnticipado(Prestamo prestamo) {
        return Result.err(new ErrorCredito.PrestamoYaPagado());
    }

    Result<CambioEstado, ErrorCredito> marcarEnMora(Prestamo prestamo, OffsetDateTime ahora) {
        return Result.err(new ErrorCredito.PrestamoYaPagado());
    }

    final Result<CambioEstado, ErrorCredito> cerrar(Prestamo prestamo, OffsetDateTime ahora) {
        return verificarCobro().flatMap(ignorado -> prestamo.cerrarSiSaldado(ahora));
    }

    void ponerAlDia(Prestamo prestamo, OffsetDateTime ahora) {}
}
