package com.projects.kapikua_api.billetera.application.port.in;

import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

public interface RecargarSaldoUseCase {
    Result<Comprobante, ErrorBilletera> recargar(UsuarioId usuarioId, Dinero monto, String tokenTarjeta,
                                                 String referenciaFormulario);
}
