package com.projects.kapikua_api.billetera.application.port.in;

import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

public interface OperacionesCreditoUseCase {
    Result<Void, ErrorBilletera> verificarIngreso(UsuarioId usuarioId, Dinero monto);

    Result<Long, ErrorBilletera> acreditarDesembolso(UsuarioId usuarioId, Dinero monto, String referencia,
                                                     String descripcion);

    Result<Dinero, ErrorBilletera> apartar(UsuarioId usuarioId, Dinero monto);

    Result<Void, ErrorBilletera> liberarApartado(UsuarioId usuarioId, Dinero monto);

    Result<Cargo, ErrorBilletera> cobrar(UsuarioId usuarioId, Dinero monto, Dinero usarApartado, boolean exigirTodo,
                                         boolean anticipado, String referencia, String descripcion);

    record Cargo(Long transaccionId, Dinero cobrado, Dinero deApartado, boolean nuevo) {
        public static final Cargo NADA = new Cargo(null, Dinero.CERO, Dinero.CERO, false);
    }
}
