package com.projects.kapikua_api.shared.evento;

import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.UsuarioId;

public final class EventosCredito {
    private EventosCredito() {}

    public record PrestamoDesembolsado(UsuarioId usuarioId, String codigoCredito) {}

    public record AtrasoDetectado(UsuarioId usuarioId, Long prestamoId, int dias) {}

    public record PrestamoPagado(UsuarioId usuarioId, Long prestamoId, boolean pagadoPorAval) {}

    public record PagoRegistrado(UsuarioId usuarioId, Long prestamoId, String codigoCredito,
                                 Dinero monto) {}
}
