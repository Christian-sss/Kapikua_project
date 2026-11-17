package com.projects.kapikua_api.billetera.domain.model;

import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Result;

final class EstadoRetenido extends EstadoBilletera {
    EstadoRetenido() {
        super("RETENIDA");
    }

    @Override
    Result<Void, ErrorBilletera> validarRecepcion(boolean esDestinoAjeno) { return Result.ok(); }

    @Override
    Result<Void, ErrorBilletera> validarSalida() { return Result.err(new ErrorBilletera.BilleteraRetenida()); }

    @Override
    Result<EstadoBilletera, ErrorBilletera> bloquear() {
        return Result.ok(BLOQUEADA);
    }

    @Override
    Result<EstadoBilletera, ErrorBilletera> retener() {
        return Result.err(new ErrorBilletera.BilleteraYaRetenida());
    }

    @Override
    Result<EstadoBilletera, ErrorBilletera> reactivar() {
        return Result.ok(ACTIVA);
    }
}
