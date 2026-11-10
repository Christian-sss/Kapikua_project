package com.projects.kapikua_api.billetera.application.port.out;

import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;

/**
 * Cargo con tarjeta para la recarga (CU-04). El formulario de la pasarela captura la tarjeta en el navegador
 * y entrega un token de un solo uso: los datos de la tarjeta nunca pasan por el servidor.
 */
public interface PasarelaPagos {

    /** Devuelve el identificador del cargo, que será la referencia única de la recarga. */
    Result<String, ErrorBilletera> cargar(Dinero monto, String token, String correo, String referencia);
}
