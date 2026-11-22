package com.projects.kapikua_api.billetera.model;

public final class TipoTransferencia implements TipoEnvio {
    @Override
    public String codigo() {
        return "TRANSFERENCIA";
    }

    @Override
    public boolean requiereCodigoDeUnUso() {
        return true;
    }
}
