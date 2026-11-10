package com.projects.kapikua_api.evaluacion.domain.error;

import com.projects.kapikua_api.shared.ErrorDominio;

/**
 * Errores de Evaluación Crediticia. Un NO_APTO no es un error: es una recomendación válida (Ok).
 * Err significa que la evaluación no se pudo hacer.
 */
public sealed interface ErrorEvaluacion extends ErrorDominio {

    /** RN-07. */
    record ConsentimientoFaltante() implements ErrorEvaluacion {
        public String codigo()  { return "EV-01"; }
        public String mensaje() { return "Debes autorizar el uso de los datos de tu billetera para evaluar tu crédito."; }
    }

    /** RN-08: no se guarda evaluación; se indica lo que falta. */
    record BilleteraSinUsoMinimo(String falta) implements ErrorEvaluacion {
        public String codigo()  { return "EV-02"; }
        public String mensaje() { return "Todavía no puedes solicitar un crédito: " + falta + "."; }
    }

    /** RN-14. */
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

    /** Error de configuración: falta un parámetro vigente de la política de crédito. */
    record ParametrosNoDisponibles() implements ErrorEvaluacion {
        public String codigo()  { return "EV-07"; }
        public String mensaje() { return "No es posible evaluar en este momento. Intenta más tarde."; }
    }

    // RN-21: evaluación del aval. Los mensajes no revelan datos crediticios del aval (CU-09, 2a).

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
