package com.projects.kapikua_api.credito.domain.model;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.shared.Result;

import java.time.OffsetDateTime;

final class PrestamoVigente extends EstadoPrestamo {
    PrestamoVigente() {
        super("VIGENTE");
    }

    @Override
    Result<Void, ErrorCredito> verificarCobro() {
        return Result.ok();
    }

    @Override
    Result<Void, ErrorCredito> verificarAnticipado(Prestamo prestamo) {
        if (prestamo.tieneCuotasVencidas()) return Result.err(new ErrorCredito.HayCuotasVencidas());
        return Result.ok();
    }

    @Override
    Result<CambioEstado, ErrorCredito> marcarEnMora(Prestamo prestamo, OffsetDateTime ahora) {
        if (!prestamo.tieneCuotasVencidas()) return Result.err(new ErrorCredito.PrestamoSinCuotasVencidas());
        return Result.ok(prestamo.cambiarEstado(EN_MORA, "Tiene cuotas vencidas", ahora));
    }
}
