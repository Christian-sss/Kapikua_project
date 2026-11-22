package com.projects.kapikua_api.billetera.model;

import com.projects.kapikua_api.billetera.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Result;

public final class TipoPagoNegocio implements TipoEnvio {
    @Override
    public String codigo() {
        return "PAGO_NEGOCIO";
    }

    @Override
    public ErrorBilletera destinatarioNoEncontrado() {
        return new ErrorBilletera.NoEsNegocio();
    }

    @Override
    public Result<Void, ErrorBilletera> validarDestinatario(Billetera destinatario) {
        return destinatario.esNegocio() ? Result.ok() : Result.err(new ErrorBilletera.NoEsNegocio());
    }

    @Override
    public String descripcion(Billetera destinatario) {
        return destinatario.getNombreNegocio();
    }
}
