package com.projects.kapikua_api.credito.application.port.out;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

import java.util.Optional;

/** Billetera: todo dinero del crédito entra y sale por aquí (RN-01, RN-02, RN-22, RN-31). */
public interface OperacionesBilletera {

    Result<Void, ErrorCredito> verificarIngreso(UsuarioId usuarioId, Dinero monto);

    /** Dentro de la transacción del desembolso. */
    Result<Long, ErrorCredito> acreditarDesembolso(UsuarioId usuarioId, Dinero monto, String referencia, String descripcion);

    Optional<UsuarioId> usuarioPorCelular(String celular);

    /** RN-22: aparta del saldo disponible hasta {@code monto}; devuelve lo que se apartó. */
    Dinero apartar(UsuarioId usuarioId, Dinero monto);

    void liberarApartado(UsuarioId usuarioId, Dinero monto);

    /**
     * Cobra hasta {@code monto}: primero de lo apartado (hasta {@code usarApartado}) y luego del saldo disponible.
     * Con {@code exigirTodo}, si no alcanza no cobra nada (Err). Idempotente por {@code referencia}.
     */
    Result<Cargo, ErrorCredito> cobrar(UsuarioId usuarioId, Dinero monto, Dinero usarApartado, boolean exigirTodo,
                                       boolean anticipado, String referencia, String descripcion);

    /** @param nuevo false si esa referencia ya se había cobrado: no se vuelve a aplicar */
    record Cargo(Long transaccionId, Dinero cobrado, Dinero deApartado, boolean nuevo) {}
}
