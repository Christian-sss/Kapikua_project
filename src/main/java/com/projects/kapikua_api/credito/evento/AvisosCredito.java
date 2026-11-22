package com.projects.kapikua_api.credito.evento;

import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.evento.EventosCredito;
import com.projects.kapikua_api.shared.puerto.Notificador;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Avisa a la pantalla del cliente cuando cambian sus cuotas o su préstamo (RNF-17): desembolso, pago,
 * atraso o préstamo pagado. Igual que en la billetera, el aviso sale solo después de confirmar la
 * transacción, para que la pantalla nunca muestre un estado que se revirtió.
 */
@Component
class AvisosCredito {
    private final Notificador notificador;

    AvisosCredito(Notificador notificador) {
        this.notificador = notificador;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void alDesembolsar(EventosCredito.PrestamoDesembolsado e) {
        avisar(e.usuarioId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void alPagar(EventosCredito.PagoRegistrado e) {
        avisar(e.usuarioId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void alAtrasarse(EventosCredito.AtrasoDetectado e) {
        avisar(e.usuarioId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void alTerminarDePagar(EventosCredito.PrestamoPagado e) {
        avisar(e.usuarioId());
    }

    private void avisar(UsuarioId usuarioId) {
        if (usuarioId != null) notificador.notificar(usuarioId, Notificador.Aviso.CREDITO);
    }
}
