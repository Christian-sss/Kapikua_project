package com.projects.kapikua_api.evaluacion.application.port.out;

import com.projects.kapikua_api.shared.UsuarioId;

import java.util.Optional;

public interface IdentidadCliente {
    record DatosCliente(String dni, boolean identidadValidada) {
        @Override
        public String toString() { return "DatosCliente[identidadValidada=" + identidadValidada + "]"; }
    }

    Optional<DatosCliente> datos(UsuarioId usuarioId);

    boolean autorizoEvaluacionConBilletera(UsuarioId usuarioId);
}
