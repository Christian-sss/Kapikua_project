package com.projects.kapikua_api.billetera.infrastructure.adapter.out.evento;

import com.projects.kapikua_api.billetera.application.port.out.BilleteraRepositoryPort;
import com.projects.kapikua_api.billetera.domain.event.CobroRecibido;
import com.projects.kapikua_api.billetera.domain.event.OperacionRegistrada;
import com.projects.kapikua_api.billetera.domain.model.Billetera;
import com.projects.kapikua_api.shared.evento.TransaccionRegistrada;
import com.projects.kapikua_api.shared.puerto.PublicadorEventos;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Traduce los eventos de dominio de Billetera a los eventos de integración que escuchan Cumplimiento
 * (TransaccionRegistrada) y Créditos (CobroRecibido), en la misma transacción.
 */
@Component
class TraductorEventosBilletera {

    private final PublicadorEventos publicador;
    private final BilleteraRepositoryPort billeteras;

    TraductorEventosBilletera(PublicadorEventos publicador, BilleteraRepositoryPort billeteras) {
        this.publicador = publicador;
        this.billeteras = billeteras;
    }

    @EventListener
    void alRegistrarOperacion(OperacionRegistrada e) {
        publicador.publicar(new TransaccionRegistrada(e.transaccionId(), e.tipo().name(), e.monto(),
                e.billeteraOrigenId(), e.billeteraDestinoId(), e.fecha()));
    }

    /** Créditos identifica al negocio por su usuario (RN-22): se agrega el titular de la billetera. */
    @EventListener
    void alRecibirCobro(CobroRecibido e) {
        billeteras.porId(e.billeteraId()).map(Billetera::getUsuarioId).ifPresent(usuario ->
                publicador.publicar(new com.projects.kapikua_api.shared.evento.CobroRecibido(
                        usuario, e.billeteraId(), e.transaccionId(), e.monto())));
    }
}
