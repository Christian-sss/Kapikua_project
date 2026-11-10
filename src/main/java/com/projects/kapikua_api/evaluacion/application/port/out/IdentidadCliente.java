package com.projects.kapikua_api.evaluacion.application.port.out;

import com.projects.kapikua_api.shared.UsuarioId;

import java.util.Optional;

/** Datos que Evaluación necesita de Identidad. */
public interface IdentidadCliente {

    /** @param dni completo, solo para consultar la Central de Riesgos */
    record DatosCliente(String dni, boolean identidadValidada) {
        @Override
        public String toString() { return "DatosCliente[identidadValidada=" + identidadValidada + "]"; }
    }

    Optional<DatosCliente> datos(UsuarioId usuarioId);

    /** RN-07: consentimiento para evaluar con los datos de la billetera. */
    boolean autorizoEvaluacionConBilletera(UsuarioId usuarioId);
}
