package com.projects.kapikua_api.evaluacion.domain.error;

import com.projects.kapikua_api.shared.ErrorDominio;

public sealed interface ErrorEvaluacion extends ErrorDominio {
    record ConsentimientoFaltante() implements ErrorEvaluacion {
        public String codigo()  { return "EV-01"; }
        public String mensaje() { return "Debes autorizar el uso de los datos de tu billetera para evaluar tu crédito."; }
    }

    record BilleteraSinUsoMinimo(String falta) implements ErrorEvaluacion {
        public String codigo()  { return "EV-02"; }
        public String mensaje() { return "Todavía no puedes solicitar un crédito: " + falta + "."; }
    }

    record CuotasFueraDeRango(int minimo, int maximo) implements ErrorEvaluacion {
        public String codigo()  { return "EV-03"; }
        public String mensaje() { return "En tu nivel puedes elegir de " + minimo + " a " + maximo + " cuotas semanales."; }
    }

    record MontoInvalido() implements ErrorEvaluacion {
        public String codigo()  { return "EV-04"; }
        public String mensaje() { return "Ingresa un monto mayor que cero."; }
    }

    record CentralNoDisponible() implements ErrorEvaluacion {
        public String codigo()  { return "EV-05"; }
        public String mensaje() { return "No pudimos consultar la Central de Riesgos. Intenta más tarde."; }
    }

    record UsuarioNoEncontrado() implements ErrorEvaluacion {
        public String codigo()  { return "EV-06"; }
        public String mensaje() { return "No encontramos tus datos."; }
    }

    record ParametrosNoDisponibles() implements ErrorEvaluacion {
        public String codigo()  { return "EV-07"; }
        public String mensaje() { return "No es posible evaluar en este momento. Intenta más tarde."; }
    }

    record AvalIdentidadNoValidada() implements ErrorEvaluacion {
        public String codigo()  { return "EV-08"; }
        public String mensaje() { return "Esa persona no puede ser tu aval. Designa a otra."; }
    }

    record AvalClasificacionNoNormal() implements ErrorEvaluacion {
        public String codigo()  { return "EV-09"; }
        public String mensaje() { return "Esa persona no puede ser tu aval. Designa a otra."; }
    }

    record AvalBloqueado() implements ErrorEvaluacion {
        public String codigo()  { return "EV-10"; }
        public String mensaje() { return "Esa persona no puede ser tu aval. Designa a otra."; }
    }
}
