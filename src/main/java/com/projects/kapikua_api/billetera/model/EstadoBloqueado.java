package com.projects.kapikua_api.billetera.model;

import com.projects.kapikua_api.billetera.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Result;

final class EstadoBloqueado extends EstadoBilletera {
    EstadoBloqueado() {
        super("BLOQUEADA");
    }

    @Override
    boolean estaBloqueada() { return true; }

    @Override
    Result<Void, ErrorBilletera> validarRecepcion(boolean esDestinoAjeno) {
        return esDestinoAjeno ? Result.err(new ErrorBilletera.DestinatarioNoEncontrado()) : validarSalida();
    }

    @Override
    Result<Void, ErrorBilletera> validarSalida() { return Result.err(new ErrorBilletera.BilleteraBloqueada()); }

    @Override
    Result<EstadoBilletera, ErrorBilletera> bloquear() {
        return Result.err(new ErrorBilletera.BilleteraYaBloqueada());
    }

    @Override
    Result<EstadoBilletera, ErrorBilletera> retener() {
        return Result.err(new ErrorBilletera.BilleteraBloqueada());
    }

    @Override
    Result<EstadoBilletera, ErrorBilletera> reactivar() {
        return Result.ok(ACTIVA);
    }
}
