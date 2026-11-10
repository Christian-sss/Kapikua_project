package com.projects.kapikua_api.cumplimiento.domain.model;

/** RN-28: las tres reglas de operaciones inusuales. */
public enum ReglaAlerta {
    /** 3 o más operaciones en 24 horas que juntas superan el tope por operación. */
    FRACCIONAMIENTO("Fraccionamiento"),
    /** Una operación de 90 % o más del tope por operación. */
    CERCA_DEL_TOPE("Cerca del tope"),
    /** 5 o más envíos (transferencias o pagos) en 10 minutos. */
    TRANSFERENCIAS_SEGUIDAS("Transferencias seguidas");

    private final String nombre;

    ReglaAlerta(String nombre) {
        this.nombre = nombre;
    }

    public String nombre() {
        return nombre;
    }
}
