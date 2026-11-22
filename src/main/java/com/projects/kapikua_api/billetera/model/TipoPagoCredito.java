package com.projects.kapikua_api.billetera.model;

record TipoPagoCredito(String codigo) implements TipoTransaccion {
    @Override
    public boolean esPagoCredito() {
        return true;
    }
}
