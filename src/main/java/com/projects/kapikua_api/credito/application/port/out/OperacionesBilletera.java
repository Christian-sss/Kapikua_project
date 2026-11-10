package com.projects.kapikua_api.credito.application.port.out;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

import java.util.Optional;

public interface OperacionesBilletera {
    Result<Void, ErrorCredito> verificarIngreso(UsuarioId usuarioId, Dinero monto);

    Result<Long, ErrorCredito> acreditarDesembolso(UsuarioId usuarioId, Dinero monto, String referencia, String descripcion);

    Optional<UsuarioId> usuarioPorCelular(String celular);

    Dinero apartar(UsuarioId usuarioId, Dinero monto);

    void liberarApartado(UsuarioId usuarioId, Dinero monto);

    Result<Cargo, ErrorCredito> cobrar(UsuarioId usuarioId, Dinero monto, Dinero usarApartado, boolean exigirTodo,
                                       boolean anticipado, String referencia, String descripcion);

    record Cargo(Long transaccionId, Dinero cobrado, Dinero deApartado, boolean nuevo) {}
}
