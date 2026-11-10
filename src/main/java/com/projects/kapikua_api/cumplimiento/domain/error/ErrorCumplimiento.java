package com.projects.kapikua_api.cumplimiento.domain.error;

import com.projects.kapikua_api.shared.ErrorDominio;

public sealed interface ErrorCumplimiento extends ErrorDominio {
    record AlertaNoEncontrada() implements ErrorCumplimiento {
        public String codigo()  { return "CM-10"; }
        public String mensaje() { return "La alerta no existe."; }
    }

    record AlertaYaRevisada() implements ErrorCumplimiento {
        public String codigo()  { return "CM-11"; }
        public String mensaje() { return "La alerta ya fue revisada; su decisión no se puede cambiar."; }
    }

    record DecisionInvalida() implements ErrorCumplimiento {
        public String codigo()  { return "CM-12"; }
        public String mensaje() { return "Elige si la alerta se descarta o se escala."; }
    }

    record ComentarioInvalido() implements ErrorCumplimiento {
        public String codigo()  { return "CM-13"; }
        public String mensaje() { return "Escribe un comentario de 5 a 255 caracteres."; }
    }

    record MotivoBloqueoInvalido() implements ErrorCumplimiento {
        public String codigo()  { return "CM-14"; }
        public String mensaje() { return "Para bloquear la billetera escribe un motivo de 5 a 150 caracteres."; }
    }

    record BloqueoSoloAlEscalar() implements ErrorCumplimiento {
        public String codigo()  { return "CM-15"; }
        public String mensaje() { return "Solo se puede bloquear la billetera al escalar la alerta."; }
    }

    record BloqueoFallido(String detalle) implements ErrorCumplimiento {
        public String codigo()  { return "CM-16"; }
        public String mensaje() { return "No se pudo bloquear la billetera: " + detalle; }
    }

    record ReclamoNoEncontrado() implements ErrorCumplimiento {
        public String codigo()  { return "CM-20"; }
        public String mensaje() { return "El reclamo no existe."; }
    }

    record MotivoRequerido() implements ErrorCumplimiento {
        public String codigo()  { return "CM-21"; }
        public String mensaje() { return "Elige el motivo del reclamo."; }
    }

    record DetalleInvalido(int maximo) implements ErrorCumplimiento {
        public String codigo()  { return "CM-22"; }
        public String mensaje() { return "Describe el problema en 10 a " + maximo + " caracteres."; }
    }

    record OperacionAjena() implements ErrorCumplimiento {
        public String codigo()  { return "CM-23"; }
        public String mensaje() { return "No encontramos esa operación en tu billetera. Revisa el número."; }
    }

    record ReclamoYaAtendido() implements ErrorCumplimiento {
        public String codigo()  { return "CM-24"; }
        public String mensaje() { return "El reclamo ya fue atendido."; }
    }

    record RespuestaInvalida() implements ErrorCumplimiento {
        public String codigo()  { return "CM-25"; }
        public String mensaje() { return "Escribe una respuesta de 10 a 500 caracteres."; }
    }

    record ProrrogaYaRegistrada() implements ErrorCumplimiento {
        public String codigo()  { return "CM-26"; }
        public String mensaje() { return "Este reclamo ya tuvo una prórroga."; }
    }

    record JustificacionInvalida() implements ErrorCumplimiento {
        public String codigo()  { return "CM-27"; }
        public String mensaje() { return "Justifica la prórroga en 10 a 255 caracteres."; }
    }

    record OperacionConcurrente() implements ErrorCumplimiento {
        public String codigo()  { return "CM-28"; }
        public String mensaje() { return "Otra persona lo revisó al mismo tiempo. Vuelve a abrirlo."; }
    }
}
