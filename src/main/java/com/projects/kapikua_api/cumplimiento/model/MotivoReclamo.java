package com.projects.kapikua_api.cumplimiento.model;

public enum MotivoReclamo {
    OPERACION("Una operación de mi billetera"),
    CREDITO("Mi crédito"),
    COBRO("Un cobro de cuota"),
    OTRO("Otro motivo");

    private final String descripcion;

    MotivoReclamo(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }
}
