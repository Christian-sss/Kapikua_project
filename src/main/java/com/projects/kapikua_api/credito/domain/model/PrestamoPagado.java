package com.projects.kapikua_api.credito.domain.model;

final class PrestamoPagado extends EstadoPrestamo {
    PrestamoPagado() {
        super("PAGADO");
    }

    @Override
    boolean estaPagado() { return true; }
}
