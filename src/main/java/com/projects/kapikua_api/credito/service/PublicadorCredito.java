package com.projects.kapikua_api.credito.service;

import com.projects.kapikua_api.credito.model.Prestamo;
import com.projects.kapikua_api.shared.evento.EventosCredito;
import com.projects.kapikua_api.shared.puerto.PublicadorEventos;
import org.springframework.stereotype.Component;

/**
 * Avisa al resto del sistema de lo que pasó con un préstamo ya guardado. Evaluación usa el desembolso, el atraso y
 * el préstamo pagado para el historial del score; AvisosCredito y la entidad aliada se enteran de los pagos. Se
 * llama dentro de la misma transacción que guarda el préstamo.
 */
@Component
public class PublicadorCredito {
    private final PublicadorEventos publicador;

    public PublicadorCredito(PublicadorEventos publicador) {
        this.publicador = publicador;
    }

    public void desembolsado(Prestamo prestamo) {
        guardado(prestamo);
        publicador.publicar(new EventosCredito.PrestamoDesembolsado(prestamo.getUsuarioId(),
                prestamo.getCodigoCreditoEfa()));
    }

    /** Un aviso por cada pago que el préstamo registró en esta operación. */
    public void pagosRegistrados(Prestamo prestamo) {
        guardado(prestamo);
        for (var pago : prestamo.pagosNuevos()) {
            publicador.publicar(new EventosCredito.PagoRegistrado(prestamo.getUsuarioId(), prestamo.getId(),
                    prestamo.getCodigoCreditoEfa(), pago.monto()));
        }
    }

    public void prestamoPagado(Prestamo prestamo) {
        guardado(prestamo);
        publicador.publicar(new EventosCredito.PrestamoPagado(prestamo.getUsuarioId(), prestamo.getId(),
                prestamo.isConPagosDeAval()));
    }

    public void atrasoDetectado(Prestamo prestamo, int dias) {
        guardado(prestamo);
        publicador.publicar(new EventosCredito.AtrasoDetectado(prestamo.getUsuarioId(), prestamo.getId(), dias));
    }

    private static void guardado(Prestamo prestamo) {
        if (prestamo.getId() == null) throw new IllegalStateException("El préstamo aún no se ha guardado");
    }
}
