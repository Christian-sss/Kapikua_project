package com.projects.kapikua_api.shared.puerto;

import com.projects.kapikua_api.shared.ErrorDominio;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

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
