package com.projects.kapikua_api.billetera.application.port.in;

import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

/** CU-04 Recargar saldo con tarjeta (RF-BI-02). */
public interface RecargarSaldoUseCase {

    /** @param tokenTarjeta token de un solo uso que entrega el formulario de la pasarela */
    Result<Comprobante, ErrorBilletera> recargar(UsuarioId usuarioId, Dinero monto, String tokenTarjeta,
                                                 String referenciaFormulario);
}
