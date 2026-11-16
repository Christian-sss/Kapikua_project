package com.projects.kapikua_api.evaluacion.domain.model;

import java.util.Map;

public record ReglasNivel(Map<Integer, CondicionesNivel> niveles, int nivelMaxConAval, int diasAtrasoMaxParaSubir,
                          int diasAtrasoBajaNivel, int diasAtrasoBloqueo) {
    public ReglasNivel {
        niveles = Map.copyOf(niveles);
        if (niveles.isEmpty()) {
            throw new IllegalArgumentException("Falta la configuración del nivel 1");
        }
        for (int nivel = 1; nivel <= niveles.size(); nivel++) {
            var condiciones = niveles.get(nivel);
            if (condiciones == null || condiciones.nivel() != nivel) {
                throw new IllegalArgumentException("Configuración inconsistente para el nivel " + nivel);
            }
        }
    }

    public CondicionesNivel nivel(int nivel) {
        var condiciones = niveles.get(nivel);
        if (condiciones == null) throw new IllegalArgumentException("Nivel sin parámetros: " + nivel);
        return condiciones;
    }

    public int nivelMaximo() {
        return niveles.size();
    }
}
