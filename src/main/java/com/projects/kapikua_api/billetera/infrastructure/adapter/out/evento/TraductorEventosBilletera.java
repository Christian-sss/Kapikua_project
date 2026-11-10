package com.projects.kapikua_api.billetera.infrastructure.adapter.out.evento;

import com.projects.kapikua_api.billetera.application.port.out.BilleteraRepositoryPort;
import com.projects.kapikua_api.billetera.domain.event.CobroRecibido;
import com.projects.kapikua_api.billetera.domain.event.OperacionRegistrada;
import com.projects.kapikua_api.billetera.domain.model.Billetera;
import com.projects.kapikua_api.shared.evento.TransaccionRegistrada;
import com.projects.kapikua_api.shared.puerto.PublicadorEventos;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

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

    @EventListener
    void alRecibirCobro(CobroRecibido e) {
        billeteras.porId(e.billeteraId()).map(Billetera::getUsuarioId).ifPresent(usuario ->
                publicador.publicar(new com.projects.kapikua_api.shared.evento.CobroRecibido(
                        usuario, e.billeteraId(), e.transaccionId(), e.monto())));
    }
}
