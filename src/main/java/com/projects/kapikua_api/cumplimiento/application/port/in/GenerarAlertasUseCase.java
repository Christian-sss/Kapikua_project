package com.projects.kapikua_api.cumplimiento.application.port.in;

import com.projects.kapikua_api.shared.evento.TransaccionRegistrada;

/** RN-28 («extend» de CU-04 a CU-06): evalúa las reglas de operaciones inusuales tras cada operación. */
public interface GenerarAlertasUseCase {

    /** Devuelve cuántas alertas nuevas registró. */
    int evaluar(TransaccionRegistrada operacion);
}
