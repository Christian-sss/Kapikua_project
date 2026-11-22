package com.projects.kapikua_api.credito.dto;

import java.util.List;

/** Lo que ve el cliente en la pantalla de crédito: su préstamo, su oferta, invitaciones y garantías. */
public record PanelCredito(DetallePrestamo prestamo, DetalleOferta oferta, List<InvitacionPendiente> invitaciones,
                           List<Garantia> garantias) {
}
