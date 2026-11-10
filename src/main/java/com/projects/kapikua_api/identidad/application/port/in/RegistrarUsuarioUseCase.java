package com.projects.kapikua_api.identidad.application.port.in;

import com.projects.kapikua_api.identidad.application.command.RegistrarUsuarioCommand;
import com.projects.kapikua_api.identidad.domain.error.RegistroError;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

public interface RegistrarUsuarioUseCase {
    Result<UsuarioId, RegistroError> registrar(RegistrarUsuarioCommand comando);
}
