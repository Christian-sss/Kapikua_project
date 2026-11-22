package com.projects.kapikua_api.credito.model;

final class PrestamoPagado extends EstadoPrestamo {
    PrestamoPagado() {
        super("PAGADO");
    }

    @Override
    boolean estaPagado() { return true; }
}
