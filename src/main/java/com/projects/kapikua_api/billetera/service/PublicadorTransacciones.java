package com.projects.kapikua_api.billetera.service;

import com.projects.kapikua_api.billetera.model.Billetera;
import com.projects.kapikua_api.billetera.model.Transaccion;
import com.projects.kapikua_api.billetera.repository.BilleteraRepository;
import com.projects.kapikua_api.shared.evento.CobroRecibido;
import com.projects.kapikua_api.shared.evento.TransaccionRegistrada;
import com.projects.kapikua_api.shared.puerto.PublicadorEventos;
import org.springframework.stereotype.Component;

/**
 * Avisa al resto del sistema de cada transacción guardada: Cumplimiento evalúa alertas con TransaccionRegistrada y
 * Crédito aparta la cuota con CobroRecibido. Se llama dentro de la misma transacción que guarda la operación.
 */
@Component
public class PublicadorTransacciones {
    private final PublicadorEventos publicador;
    private final BilleteraRepository billeteras;

    public PublicadorTransacciones(PublicadorEventos publicador, BilleteraRepository billeteras) {
        this.publicador = publicador;
        this.billeteras = billeteras;
    }

    public void publicar(Transaccion t) {
        if (t.id() == null) throw new IllegalStateException("La transacción aún no se ha guardado");
        var origen = t.movimientos().stream().filter(m -> m.monto().signum() < 0)
                .map(Transaccion.Movimiento::billeteraId).findFirst().orElse(null);
        var destino = t.movimientos().stream().filter(m -> m.monto().signum() > 0)
                .map(Transaccion.Movimiento::billeteraId).findFirst().orElse(null);

        publicador.publicar(new TransaccionRegistrada(t.id(), t.tipo().codigo(), t.monto(), origen, destino, t.fecha()));
        if (t.tipo().generaCobroRecibido() && destino != null) {
            billeteras.porId(destino).map(Billetera::getUsuarioId).ifPresent(usuario ->
                    publicador.publicar(new CobroRecibido(usuario, destino, t.id(), t.monto())));
        }
    }
}
