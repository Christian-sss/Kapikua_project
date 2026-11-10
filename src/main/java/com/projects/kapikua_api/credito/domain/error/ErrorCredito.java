package com.projects.kapikua_api.credito.domain.error;

import com.projects.kapikua_api.shared.ErrorDominio;

public sealed interface ErrorCredito extends ErrorDominio {
    record PrestamoActivoExistente() implements ErrorCredito {
        public String codigo()  { return "CR-01"; }
        public String mensaje() { return "Ya tienes un préstamo vigente. Podrás solicitar otro cuando lo termines de pagar."; }
    }

    record OfertaEnCurso() implements ErrorCredito {
        public String codigo()  { return "CR-02"; }
        public String mensaje() { return "Ya tienes una solicitud en curso. Revísala antes de pedir otra."; }
    }

    record NoSePudoEvaluar(String detalle) implements ErrorCredito {
        public String codigo()  { return "CR-03"; }
        public String mensaje() { return detalle; }
    }

    record OfertaNoEncontrada() implements ErrorCredito {
        public String codigo()  { return "CR-04"; }
        public String mensaje() { return "No encontramos esa oferta."; }
    }

    record OfertaNoVigente() implements ErrorCredito {
        public String codigo()  { return "CR-05"; }
        public String mensaje() { return "La oferta venció. Puedes volver a solicitar un crédito."; }
    }

    record OfertaEstadoInvalido(String estado) implements ErrorCredito {
        public String codigo()  { return "CR-06"; }
        public String mensaje() { return "La oferta no permite esta acción (está " + estado + ")."; }
    }

    record AvalNoRequerido() implements ErrorCredito {
        public String codigo()  { return "CR-07"; }
        public String mensaje() { return "Tu nivel ya no necesita aval."; }
    }

    record AvalRequerido() implements ErrorCredito {
        public String codigo()  { return "CR-08"; }
        public String mensaje() { return "Necesitas que tu aval acepte antes de continuar."; }
    }

    record AvalMismoDeudor() implements ErrorCredito {
        public String codigo()  { return "CR-09"; }
        public String mensaje() { return "No puedes ser tu propio aval."; }
    }

    record AvalYaInvitado() implements ErrorCredito {
        public String codigo()  { return "CR-10"; }
        public String mensaje() { return "Ya invitaste a esa persona para esta solicitud."; }
    }

    record AvalEnCurso() implements ErrorCredito {
        public String codigo()  { return "CR-11"; }
        public String mensaje() { return "Ya tienes un aval invitado o aceptado. Espera su respuesta."; }
    }

    record AvalesAgotados(int maximo) implements ErrorCredito {
        public String codigo()  { return "CR-12"; }
        public String mensaje() { return "Ya invitaste a " + maximo + " avales. Puedes volver a solicitar con una evaluación nueva."; }
    }

    record AvalNoEncontrado() implements ErrorCredito {
        public String codigo()  { return "CR-13"; }
        public String mensaje() { return "Ese celular no tiene una billetera Kapikua."; }
    }

    record AvalNoCalifica() implements ErrorCredito {
        public String codigo()  { return "CR-14"; }
        public String mensaje() { return "Esa persona no puede ser tu aval. Designa a otra."; }
    }

    record InvitacionNoEncontrada() implements ErrorCredito {
        public String codigo()  { return "CR-15"; }
        public String mensaje() { return "No encontramos esa invitación, o ya no está pendiente."; }
    }

    record CodigoNoValido(String detalle) implements ErrorCredito {
        public String codigo()  { return "CR-16"; }
        public String mensaje() { return detalle; }
    }

    record CargoNoAutorizado() implements ErrorCredito {
        public String codigo()  { return "CR-17"; }
        public String mensaje() { return "Para aceptar, autoriza el cobro automático de tus cuotas."; }
    }

    record EfaNoDisponible() implements ErrorCredito {
        public String codigo()  { return "CR-18"; }
        public String mensaje() { return "La entidad aliada no responde. Intenta más tarde."; }
    }

    record DesembolsoNoPermitido(String detalle) implements ErrorCredito {
        public String codigo()  { return "CR-19"; }
        public String mensaje() { return detalle; }
    }

    record MontoInvalido() implements ErrorCredito {
        public String codigo()  { return "CR-20"; }
        public String mensaje() { return "Ingresa un monto mayor que cero y de hasta S/ 3 300."; }
    }

    record CuotasFueraDeRango(int minimo, int maximo) implements ErrorCredito {
        public String codigo()  { return "CR-21"; }
        public String mensaje() { return "En tu nivel puedes elegir de " + minimo + " a " + maximo + " cuotas semanales."; }
    }

    record CuotaMinima(String cuotaMinima) implements ErrorCredito {
        public String codigo()  { return "CR-22"; }
        public String mensaje() { return "La cuota resultante es menor que la cuota mínima de " + cuotaMinima + "."; }
    }

    record PrestamoNoEncontrado() implements ErrorCredito {
        public String codigo()  { return "CR-23"; }
        public String mensaje() { return "No tienes un préstamo vigente."; }
    }

    record ConsentimientoRequerido() implements ErrorCredito {
        public String codigo()  { return "CR-25"; }
        public String mensaje() { return "Para evaluar tu solicitud debes autorizar el uso de los datos de tu billetera "
                + "y su envío a la entidad aliada."; }
    }

    record GarantiaNoAutorizada() implements ErrorCredito {
        public String codigo()  { return "CR-27"; }
        public String mensaje() { return "Para aceptar, autoriza el uso de tus datos y la retención de tus ventas "
                + "si la persona que respaldas no paga."; }
    }

    record AvalCruzado() implements ErrorCredito {
        public String codigo()  { return "CR-26"; }
        public String mensaje() { return "No pueden ser avales el uno del otro. Designa a otra persona."; }
    }

    record HayCuotasVencidas() implements ErrorCredito {
        public String codigo()  { return "CR-28"; }
        public String mensaje() { return "Paga primero tus cuotas vencidas; después puedes adelantar pagos."; }
    }

    record MontoAnticipadoInvalido(String capitalPendiente) implements ErrorCredito {
        public String codigo()  { return "CR-29"; }
        public String mensaje() { return "Para un pago parcial ingresa un monto mayor que cero y menor que tu capital pendiente ("
                + capitalPendiente + "). Para pagar todo, elige pago total."; }
    }

    record SaldoInsuficienteParaPagar(String necesario) implements ErrorCredito {
        public String codigo()  { return "CR-30"; }
        public String mensaje() { return "No tienes saldo disponible suficiente. Necesitas " + necesario + "."; }
    }

    record NadaQuePagar() implements ErrorCredito {
        public String codigo()  { return "CR-31"; }
        public String mensaje() { return "No tienes cuotas por pagar."; }
    }

    record ParametrosNoDisponibles() implements ErrorCredito {
        public String codigo()  { return "CR-24"; }
        public String mensaje() { return "No es posible operar en este momento. Intenta más tarde."; }
    }
}
