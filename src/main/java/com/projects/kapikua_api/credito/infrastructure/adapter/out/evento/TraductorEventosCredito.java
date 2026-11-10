package com.projects.kapikua_api.credito.infrastructure.adapter.out.evento;

import com.projects.kapikua_api.credito.domain.event.AtrasoDetectado;
import com.projects.kapikua_api.credito.domain.event.PagoAplicado;
import com.projects.kapikua_api.credito.domain.event.PrestamoDesembolsado;
import com.projects.kapikua_api.credito.domain.event.PrestamoPagado;
import com.projects.kapikua_api.shared.evento.EventosCredito;
import com.projects.kapikua_api.shared.puerto.PublicadorEventos;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Traduce los eventos de dominio de Créditos a los eventos de integración, en la misma transacción. */
@Component
class TraductorEventosCredito {

    private final PublicadorEventos publicador;

    TraductorEventosCredito(PublicadorEventos publicador) {
        this.publicador = publicador;
    }

    @EventListener
    void alDesembolsar(PrestamoDesembolsado e) {
        publicador.publicar(new EventosCredito.PrestamoDesembolsado(e.usuarioId(), e.codigoCredito()));
    }

    @EventListener
    void alAtrasarse(AtrasoDetectado e) {
        publicador.publicar(new EventosCredito.AtrasoDetectado(e.usuarioId(), e.prestamoId(), e.dias()));
    }

    @EventListener
    void alPagarse(PrestamoPagado e) {
        publicador.publicar(new EventosCredito.PrestamoPagado(e.usuarioId(), e.prestamoId(), e.pagadoPorAval()));
    }

    /** Cada pago se informa a la entidad aliada después del commit (OyentesCobranza). */
    @EventListener
    void alAplicarPago(PagoAplicado e) {
        publicador.publicar(new EventosCredito.PagoRegistrado(e.usuarioId(), e.prestamoId(), e.codigoCredito(), e.monto()));
    }
}
