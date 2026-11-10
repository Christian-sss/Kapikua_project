package com.projects.kapikua_api.shared.puerto;

import com.projects.kapikua_api.shared.ErrorDominio;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

/**
 * Autenticación reforzada para operaciones de riesgo (RNF-04, Res. SBS 504-2021).
 * El código vence a los 5 minutos, sirve una sola vez y queda ligado al detalle de la operación
 * (por ejemplo, destino y monto): no sirve para confirmar otra operación distinta.
 * Lo implementa Identidad.
 */
public interface CodigoUnSoloUso {

    enum Proposito { TRANSFERENCIA, ACEPTAR_OFERTA, ACEPTAR_AVAL, CAMBIO_DATOS }

    String generar(UsuarioId usuarioId, Proposito proposito, String detalleOperacion);

    Result<Void, CodigoError> validar(UsuarioId usuarioId, Proposito proposito, String detalleOperacion, String codigo);

    sealed interface CodigoError extends ErrorDominio {

        record Invalido() implements CodigoError {
            public String codigo()  { return "CO-01"; }
            public String mensaje() { return "El código no es correcto."; }
        }

        record Vencido() implements CodigoError {
            public String codigo()  { return "CO-02"; }
            public String mensaje() { return "El código venció o ya se usó. Pide uno nuevo."; }
        }
    }
}
