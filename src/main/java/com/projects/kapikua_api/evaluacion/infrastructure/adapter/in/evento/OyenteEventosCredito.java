package com.projects.kapikua_api.evaluacion.infrastructure.adapter.in.evento;

import com.projects.kapikua_api.evaluacion.application.port.in.ActualizarHistorialUseCase;
import com.projects.kapikua_api.shared.evento.EventosCredito.AtrasoDetectado;
import com.projects.kapikua_api.shared.evento.EventosCredito.PrestamoDesembolsado;
import com.projects.kapikua_api.shared.evento.EventosCredito.PrestamoPagado;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class OyenteEventosCredito {
    private final ActualizarHistorialUseCase historial;

    OyenteEventosCredito(ActualizarHistorialUseCase historial) {
        this.historial = historial;
    }

    @EventListener
    void alDesembolsar(PrestamoDesembolsado e) {
        historial.registrarDesembolso(e.usuarioId());
    }

    @EventListener
    void alAtrasarse(AtrasoDetectado e) {
        historial.registrarAtraso(e.usuarioId(), e.dias());
    }

    @EventListener
    void alPagarse(PrestamoPagado e) {
        historial.registrarCreditoPagado(e.usuarioId(), e.pagadoPorAval());
    }
}
