package com.projects.kapikua_api.identidad.application.port.in;

import com.projects.kapikua_api.identidad.application.command.RegistrarUsuarioCommand;
import com.projects.kapikua_api.identidad.domain.error.RegistroError;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

/** CU-01 Registrarse y abrir billetera (RF-ID-01). */
public interface RegistrarUsuarioUseCase {

    Result<UsuarioId, RegistroError> registrar(RegistrarUsuarioCommand comando);
}
