package com.projects.kapikua_api.credito.domain.model;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.shared.Result;

import java.time.OffsetDateTime;

final class PrestamoEnMora extends EstadoPrestamo {
    PrestamoEnMora() {
        super("EN_MORA");
    }

    @Override
    Result<Void, ErrorCredito> verificarCobro() {
        return Result.ok();
    }

    @Override
    Result<Void, ErrorCredito> verificarAnticipado(Prestamo prestamo) {
        return Result.err(new ErrorCredito.HayCuotasVencidas());
    }

    @Override
    Result<CambioEstado, ErrorCredito> marcarEnMora(Prestamo prestamo, OffsetDateTime ahora) {
        return Result.err(new ErrorCredito.PrestamoYaEnMora());
    }

    @Override
    void ponerAlDia(Prestamo prestamo, OffsetDateTime ahora) {
        prestamo.cambiarEstado(VIGENTE, "Se saldaron las cuotas vencidas", ahora);
    }
}
