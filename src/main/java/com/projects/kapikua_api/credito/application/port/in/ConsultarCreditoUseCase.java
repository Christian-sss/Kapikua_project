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

/** CU-12 Consultar préstamo y cronograma, y el estado de la solicitud y de las invitaciones de aval. */
public interface ConsultarCreditoUseCase {

    Panel panel(UsuarioId usuarioId);

    Result<DetalleOferta, ErrorCredito> oferta(UsuarioId usuarioId, Long ofertaId);

    /**
     * @param prestamo  el préstamo vigente o en mora; null si no tiene
     * @param oferta    la solicitud más reciente si no terminó en préstamo; null si no hay
     */
    record Panel(DetallePrestamo prestamo, DetalleOferta oferta, List<Invitacion> invitaciones, List<Garantia> garantias) {}

    /**
     * CU-12, 1b: lo que ve el aval del crédito que respalda, sin datos de la billetera del deudor.
     * @param avisadoEl fecha del aviso por atraso (día 9); null si no hubo
     */
    record Garantia(Long prestamoId, String deudor, EstadoAval estadoAval, EstadoPrestamo estadoPrestamo, Dinero monto,
                    int cuotasVencidas, Dinero vencido, OffsetDateTime avisadoEl) {}

    record Invitacion(Long ofertaId, String deudor, Dinero monto, OffsetDateTime vence) {}

    /**
     * Condiciones sugeridas por Kapikua hasta que la entidad aliada confirma; desde ahí, las de la entidad.
     */
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
