package com.projects.kapikua_api.credito.application.port.in;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.credito.domain.model.CondicionesCredito;
import com.projects.kapikua_api.credito.domain.model.ConstanciaAceptacion;
import com.projects.kapikua_api.credito.domain.model.Cuota;
import com.projects.kapikua_api.credito.domain.model.EstadoAval;
import com.projects.kapikua_api.credito.domain.model.EstadoOferta;
import com.projects.kapikua_api.credito.domain.model.EstadoPrestamo;
import com.projects.kapikua_api.evaluacion.domain.model.Recomendacion;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public interface ConsultarCreditoUseCase {
    Panel panel(UsuarioId usuarioId);

    Result<DetalleOferta, ErrorCredito> oferta(UsuarioId usuarioId, Long ofertaId);

    record Panel(DetallePrestamo prestamo, DetalleOferta oferta, List<Invitacion> invitaciones, List<Garantia> garantias) {}

    record Garantia(Long prestamoId, String deudor, EstadoAval estadoAval, EstadoPrestamo estadoPrestamo, Dinero monto,
                    int cuotasVencidas, Dinero vencido, OffsetDateTime avisadoEl) {}

    record Invitacion(Long ofertaId, String deudor, Dinero monto, OffsetDateTime vence) {}

    record DetalleOferta(Long id, EstadoOferta estado, Recomendacion recomendacion, String motivo,
                         Dinero monto, int cuotas, BigDecimal tea, Dinero cuota, boolean condicionesConfirmadas,
                         boolean requiereAval, List<AvalVista> avales, LocalDate vigenteHasta, String motivoDenegacion,
                         boolean puedeDesignarAval, boolean puedeEnviarAEntidad, boolean puedeAceptar,
                         CondicionesCredito condicionesEfa) {}

    record AvalVista(String nombre, EstadoAval estado, OffsetDateTime vence) {}

    record DetallePrestamo(Long id, EstadoPrestamo estado, String codigoCredito, Dinero monto, BigDecimal tea,
                           BigDecimal tcea, int porcentajeRetencion, Dinero capitalPendiente,
                           ConstanciaAceptacion constancia, String aval, List<Cuota> cuotas,
                           boolean cargoAutorizado, Dinero apartado, int cuotasVencidas, Dinero vencido) {}
}
