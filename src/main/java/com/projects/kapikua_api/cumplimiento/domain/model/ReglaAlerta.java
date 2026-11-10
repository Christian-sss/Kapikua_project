package com.projects.kapikua_api.cumplimiento.domain.model;

public enum ReglaAlerta {
    FRACCIONAMIENTO("Fraccionamiento"),
    CERCA_DEL_TOPE("Cerca del tope"),
    TRANSFERENCIAS_SEGUIDAS("Transferencias seguidas");

    private final String nombre;

    ReglaAlerta(String nombre) {
        this.nombre = nombre;
    }

    public String nombre() {
        return nombre;
    }
}
