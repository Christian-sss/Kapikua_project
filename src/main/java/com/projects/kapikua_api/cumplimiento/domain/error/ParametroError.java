package com.projects.kapikua_api.cumplimiento.domain.error;

import com.projects.kapikua_api.shared.ErrorDominio;

import java.time.LocalDate;

/** Error de configuración: falta registrar un valor vigente en cumplimiento.parametro. */
public sealed interface ParametroError extends ErrorDominio {

    record NoVigente(String codigoParametro, LocalDate fecha) implements ParametroError {
        public String codigo()  { return "CM-01"; }
        public String mensaje() { return "No hay un valor vigente del parámetro " + codigoParametro + " al " + fecha + "."; }
    }
}
