package com.projects.kapikua_api.cumplimiento.application.port.in;

import com.projects.kapikua_api.shared.evento.TransaccionRegistrada;

public interface GenerarAlertasUseCase {
    int evaluar(TransaccionRegistrada operacion);
}
