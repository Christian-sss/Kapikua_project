package com.projects.kapikua_api.credito.model;

import com.projects.kapikua_api.credito.error.ErrorCredito;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;

import java.time.LocalDate;
import java.time.OffsetDateTime;

final class CuotaPendiente extends EstadoCuota {
    CuotaPendiente() {
        super("PENDIENTE");
    }

    @Override
    boolean esPendiente() { return true; }

    @Override
    Result<Cuota.PagoImputado, ErrorCredito> imputar(Cuota cuota, Dinero monto, Dinero deApartado, LocalDate hoy) {
        return cuota.aplicarImputacion(monto, deApartado, hoy);
    }

    @Override
    Result<CambioEstado, ErrorCredito> vencer(Cuota cuota, LocalDate hoy, OffsetDateTime ahora) {
        if (hoy.isBefore(cuota.getFechaVencimiento())) {
            return Result.err(new ErrorCredito.CuotaAunNoVence(cuota.getNumero()));
        }
        return Result.ok(cuota.cambiarEstado(VENCIDA, "La cuota llegó a su fecha de vencimiento", ahora));
    }

    @Override
    Result<CambioEstado, ErrorCredito> anular(Cuota cuota, OffsetDateTime ahora) {
        return verificarReprogramable(cuota).map(ignorado -> cuota.aplicarAnulacion(ahora));
    }

    @Override
    Result<Void, ErrorCredito> verificarReprogramable(Cuota cuota) {
        return cuota.getMontoPagado().esCero() ? Result.ok() : Result.err(errorEstado(cuota));
    }
}
