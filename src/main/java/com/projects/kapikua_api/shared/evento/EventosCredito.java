package com.projects.kapikua_api.shared.evento;

import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.UsuarioId;

/**
 * Eventos de integración de Créditos (contrato publicado): Evaluación los escucha para mover la escalera
 * (RN-26, RN-32). Los produce el traductor de Créditos a partir de sus eventos de dominio, así Evaluación no
 * depende del modelo de Créditos.
 */
public final class EventosCredito {

    private EventosCredito() {}

    /** Se desembolsó un préstamo: el atraso del crédito en curso vuelve a cero. */
    public record PrestamoDesembolsado(UsuarioId usuarioId, String codigoCredito) {}

    /** Cada día que un préstamo sigue con cuotas vencidas (CU-13). */
    public record AtrasoDetectado(UsuarioId usuarioId, Long prestamoId, int dias) {}

    /** El préstamo quedó pagado. {@code pagadoPorAval}: el aval tuvo que pagar cuotas (RN-32). */
    public record PrestamoPagado(UsuarioId usuarioId, Long prestamoId, boolean pagadoPorAval) {}

    /** Se cobró dinero del crédito: después del commit se transfiere a la entidad aliada. */
    public record PagoRegistrado(UsuarioId usuarioId, Long prestamoId, String codigoCredito,
                                 Dinero monto) {}
}
