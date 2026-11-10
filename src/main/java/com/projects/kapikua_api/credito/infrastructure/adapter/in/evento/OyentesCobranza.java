package com.projects.kapikua_api.credito.infrastructure.adapter.in.evento;

import com.projects.kapikua_api.credito.application.port.in.CobranzaUseCase;
import com.projects.kapikua_api.credito.application.port.out.EntidadFinanciera;
import com.projects.kapikua_api.shared.evento.CobroRecibido;
import com.projects.kapikua_api.shared.evento.EventosCredito.PagoRegistrado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Oyentes de la cobranza. */
@Component
class OyentesCobranza {

    private static final Logger log = LoggerFactory.getLogger(OyentesCobranza.class);

    private final CobranzaUseCase cobranza;
    private final EntidadFinanciera entidad;

    OyentesCobranza(CobranzaUseCase cobranza, EntidadFinanciera entidad) {
        this.cobranza = cobranza;
        this.entidad = entidad;
    }

    /**
     * RN-22: con cada cobro (pago a negocio o transferencia recibida) se aparta para la cuota, en la MISMA
     * transacción del cobro: la retención y el cobro se confirman o se revierten juntos.
     */
    @EventListener
    void alRecibirCobro(CobroRecibido e) {
        cobranza.aplicarRetencion(e.usuarioId(), e.transaccionId(), e.monto());
    }

    /**
     * CU-11 y CU-13: lo cobrado se transfiere a la entidad aliada DESPUÉS del commit (es un sistema externo).
     * Si la entidad no responde, se registra para conciliar; el pago ya quedó cobrado en Kapikua.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void alRegistrarPago(PagoRegistrado e) {
        var r = entidad.registrarPago(e.codigoCredito(), e.monto());
        if (!r.esOk()) log.error("Conciliar: no se informó a la entidad aliada el pago de {} del crédito {}", e.monto(), e.codigoCredito());
    }
}
