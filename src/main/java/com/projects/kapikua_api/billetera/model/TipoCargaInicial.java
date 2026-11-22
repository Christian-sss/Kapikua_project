package com.projects.kapikua_api.billetera.model;

/**
 * Saldo inicial de una billetera. El sistema no tiene recarga: estas transacciones solo las crean
 * los scripts de datos (demostración e instalación). Conserva el código RECARGA de la base para
 * poder leer el historial existente.
 */
public final class TipoCargaInicial implements TipoTransaccion {
    @Override
    public String codigo() {
        return "RECARGA";
    }
}
