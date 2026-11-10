package com.projects.kapikua_api.billetera.application.port.in;

import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

public interface EnviarDineroUseCase {
    Result<VistaPrevia, ErrorBilletera> prepararTransferencia(UsuarioId origen, String celularDestino,
                                                              Dinero monto, String referencia);

    Result<Comprobante, ErrorBilletera> transferir(UsuarioId origen, String celularDestino, Dinero monto,
                                                   String referencia, String codigo);

    Result<VistaPrevia, ErrorBilletera> prepararPagoNegocio(UsuarioId origen, String celularNegocio,
                                                            Dinero monto, String referencia);

    Result<Comprobante, ErrorBilletera> pagarNegocio(UsuarioId origen, String celularNegocio, Dinero monto,
                                                     String referencia);

    record VistaPrevia(String destinatario, String celularEnmascarado, Dinero monto, String referencia,
                       String codigoMostrado) {
    }
}
