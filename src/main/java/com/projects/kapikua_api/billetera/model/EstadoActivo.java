package com.projects.kapikua_api.billetera.model;

import com.projects.kapikua_api.billetera.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Result;

final class EstadoActivo extends EstadoBilletera {
    EstadoActivo() {
        super("ACTIVA");
    }

    @Override
    boolean estaActiva() { return true; }

    @Override
    Result<Void, ErrorBilletera> validarRecepcion(boolean esDestinoAjeno) { return Result.ok(); }

    @Override
    Result<Void, ErrorBilletera> validarSalida() { return Result.ok(); }

    @Override
    Result<EstadoBilletera, ErrorBilletera> bloquear() {
        return Result.ok(BLOQUEADA);
    }

    @Override
    Result<EstadoBilletera, ErrorBilletera> retener() {
        return Result.ok(RETENIDA);
    }

    @Override
    Result<EstadoBilletera, ErrorBilletera> reactivar() {
        return Result.err(new ErrorBilletera.BilleteraYaActiva());
    }
}
