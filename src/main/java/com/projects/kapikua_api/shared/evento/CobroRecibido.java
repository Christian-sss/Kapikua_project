package com.projects.kapikua_api.shared.evento;

import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.UsuarioId;

public record CobroRecibido(UsuarioId usuarioId, Long billeteraId, Long transaccionId, Dinero monto) {}
