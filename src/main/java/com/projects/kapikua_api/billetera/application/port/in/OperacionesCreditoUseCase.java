package com.projects.kapikua_api.billetera.application.port.in;

import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

/**
 * Operaciones que Créditos hace sobre la billetera. Todo dinero del crédito pasa por aquí, para que los topes
 * y el saldo se controlen en un solo lugar. Las que escriben corren dentro de la transacción de Créditos.
 */
public interface OperacionesCreditoUseCase {

    /** Antes de pedir el desembolso a la entidad aliada: RN-01 y RN-02 (que el saldo no supere el máximo). */
    Result<Void, ErrorBilletera> verificarIngreso(UsuarioId usuarioId, Dinero monto);

    /**
     * RN-31: acredita el desembolso como dinero que entra desde la cuenta de la entidad aliada.
     * @return id de la transacción DESEMBOLSO
     */
    Result<Long, ErrorBilletera> acreditarDesembolso(UsuarioId usuarioId, Dinero monto, String referencia,
                                                     String descripcion);

    /** RN-22: aparta para la cuota hasta {@code monto} del saldo disponible. Devuelve lo que se apartó. */
    Result<Dinero, ErrorBilletera> apartar(UsuarioId usuarioId, Dinero monto);

    Result<Void, ErrorBilletera> liberarApartado(UsuarioId usuarioId, Dinero monto);

    /**
     * Cobra para el crédito hasta {@code monto}: primero de lo apartado (hasta {@code usarApartado}), luego del saldo
     * disponible. Registra una transacción PAGO_CUOTA o PAGO_ANTICIPADO. Idempotente por {@code referencia}.
     *
     * @param exigirTodo si no alcanza para el monto completo, no cobra nada y devuelve SaldoInsuficiente
     */
    Result<Cargo, ErrorBilletera> cobrar(UsuarioId usuarioId, Dinero monto, Dinero usarApartado, boolean exigirTodo,
                                         boolean anticipado, String referencia, String descripcion);

    /**
     * @param transaccionId null si no se cobró nada
     * @param nuevo         false si la referencia ya se había cobrado antes (reintento): no hay que volver a aplicarlo
     */
    record Cargo(Long transaccionId, Dinero cobrado, Dinero deApartado, boolean nuevo) {
        public static final Cargo NADA = new Cargo(null, Dinero.CERO, Dinero.CERO, false);
    }
}
