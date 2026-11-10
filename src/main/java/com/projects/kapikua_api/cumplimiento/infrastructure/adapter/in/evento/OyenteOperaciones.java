package com.projects.kapikua_api.cumplimiento.infrastructure.adapter.in.evento;

import com.projects.kapikua_api.cumplimiento.application.port.in.GenerarAlertasUseCase;
import com.projects.kapikua_api.shared.evento.TransaccionRegistrada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * RN-28: las reglas se evalúan DESPUÉS de confirmar la operación, para que una falla al generar la alerta
 * nunca revierta el movimiento de dinero del cliente.
 */
@Component
class OyenteOperaciones {

    private static final Logger log = LoggerFactory.getLogger(OyenteOperaciones.class);

    private final GenerarAlertasUseCase alertas;

    OyenteOperaciones(GenerarAlertasUseCase alertas) {
        this.alertas = alertas;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void alRegistrarOperacion(TransaccionRegistrada operacion) {
        try {
            var nuevas = alertas.evaluar(operacion);
            if (nuevas > 0) log.info("Operación {}: {} alerta(s) de operación inusual", operacion.transaccionId(), nuevas);
        } catch (RuntimeException e) {
            log.error("No se pudieron evaluar las reglas de alerta de la operación {}", operacion.transaccionId(), e);
        }
    }
}
