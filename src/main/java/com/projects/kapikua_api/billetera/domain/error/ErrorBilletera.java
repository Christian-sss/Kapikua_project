package com.projects.kapikua_api.billetera.domain.error;

import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.ErrorDominio;

public sealed interface ErrorBilletera extends ErrorDominio {
    record BilleteraNoEncontrada() implements ErrorBilletera {
        public String codigo()  { return "BI-01"; }
        public String mensaje() { return "No encontramos tu billetera."; }
    }

    record BilleteraBloqueada() implements ErrorBilletera {
        public String codigo()  { return "BI-02"; }
        public String mensaje() { return "La billetera está bloqueada. Comunícate con atención al cliente."; }
    }

    record DestinatarioNoEncontrado() implements ErrorBilletera {
        public String codigo()  { return "BI-03"; }
        public String mensaje() { return "Ese número de celular no tiene una billetera Kapikua activa."; }
    }

    record NoEsNegocio() implements ErrorBilletera {
        public String codigo()  { return "BI-04"; }
        public String mensaje() { return "Ese número no corresponde a un negocio registrado en Kapikua."; }
    }

    record OperacionConSiMismo() implements ErrorBilletera {
        public String codigo()  { return "BI-05"; }
        public String mensaje() { return "No puedes enviarte dinero a tu propia billetera."; }
    }

    record MontoInvalido() implements ErrorBilletera {
        public String codigo()  { return "BI-06"; }
        public String mensaje() { return "Ingresa un monto mayor que cero, con hasta 2 decimales."; }
    }

    record SaldoInsuficiente() implements ErrorBilletera {
        public String codigo()  { return "BI-07"; }
        public String mensaje() { return "No tienes saldo disponible suficiente."; }
    }

    record TopePorOperacion(Dinero tope) implements ErrorBilletera {
        public String codigo()  { return "BI-08"; }
        public String mensaje() { return "Cada operación puede ser de hasta " + tope + "."; }
    }

    record TopeSaldoPropio(Dinero puedeRecibir) implements ErrorBilletera {
        public String codigo()  { return "BI-09"; }
        public String mensaje() { return "Tu saldo superaría el máximo permitido. Hoy puedes recibir hasta " + puedeRecibir + "."; }
    }

    record DestinoNoPuedeRecibir() implements ErrorBilletera {
        public String codigo()  { return "BI-10"; }
        public String mensaje() { return "El destinatario no puede recibir ese monto por el límite de saldo de la SBS."; }
    }

    record TopeRecargasMes(Dinero disponible) implements ErrorBilletera {
        public String codigo()  { return "BI-11"; }
        public String mensaje() { return "Superas el límite de recargas del mes. Te quedan " + disponible + "."; }
    }

    record TopePagosMes(Dinero disponible) implements ErrorBilletera {
        public String codigo()  { return "BI-12"; }
        public String mensaje() { return "Superas el límite mensual de pagos y transferencias. Te quedan " + disponible + "."; }
    }

    record PagoRechazado(String motivo) implements ErrorBilletera {
        public String codigo()  { return "BI-13"; }
        public String mensaje() { return "La pasarela rechazó el pago: " + motivo; }
    }

    record CodigoNoValido(String detalle) implements ErrorBilletera {
        public String codigo()  { return "BI-14"; }
        public String mensaje() { return detalle; }
    }

    record ReferenciaInvalida() implements ErrorBilletera {
        public String codigo()  { return "BI-15"; }
        public String mensaje() { return "La operación no es válida. Vuelve a empezar."; }
    }

    record CelularInvalido() implements ErrorBilletera {
        public String codigo()  { return "BI-17"; }
        public String mensaje() { return "El celular tiene 9 dígitos y empieza con 9."; }
    }

    record OperacionConcurrente() implements ErrorBilletera {
        public String codigo()  { return "BI-18"; }
        public String mensaje() { return "Hay otra operación en curso en tu billetera. Intenta de nuevo en unos segundos."; }
    }

    record ParametrosNoDisponibles() implements ErrorBilletera {
        public String codigo()  { return "BI-16"; }
        public String mensaje() { return "No es posible operar en este momento. Intenta más tarde."; }
    }
}
