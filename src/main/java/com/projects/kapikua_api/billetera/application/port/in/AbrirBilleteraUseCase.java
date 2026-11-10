package com.projects.kapikua_api.billetera.application.port.in;

import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

/** CU-01, paso 8: se abre al registrarse el cliente (evento UsuarioRegistrado). */
public interface AbrirBilleteraUseCase {

    Result<Long, ErrorBilletera> abrir(UsuarioId usuarioId, String celular, String nombreNegocio);
}
