package com.projects.kapikua_api.billetera.application.port.in;

import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

/**
 * CU-05 Transferir por celular y CU-06 Pagar a un negocio. Cada uno en dos pasos:
 * preparar (valida y muestra a quién se envía) y confirmar (mueve el dinero).
 * La {@code referencia} es un UUID que genera el formulario: repetir la confirmación
 * con la misma referencia devuelve el comprobante original sin mover dinero otra vez (RNF-07).
 */
public interface EnviarDineroUseCase {

    /**
     * Genera el código de un solo uso ligado al destino, el monto y la referencia (RNF-04).
     */
    Result<VistaPrevia, ErrorBilletera> prepararTransferencia(UsuarioId origen, String celularDestino,
                                                              Dinero monto, String referencia);

    Result<Comprobante, ErrorBilletera> transferir(UsuarioId origen, String celularDestino, Dinero monto,
                                                   String referencia, String codigo);

    Result<VistaPrevia, ErrorBilletera> prepararPagoNegocio(UsuarioId origen, String celularNegocio,
                                                            Dinero monto, String referencia);

    Result<Comprobante, ErrorBilletera> pagarNegocio(UsuarioId origen, String celularNegocio, Dinero monto,
                                                     String referencia);

    /**
     * @param codigoMostrado en la v1 no hay SMS: el código se muestra en pantalla (vacío en el pago a negocio)
     */
    record VistaPrevia(String destinatario, String celularEnmascarado, Dinero monto, String referencia,
                       String codigoMostrado) {
    }
}
