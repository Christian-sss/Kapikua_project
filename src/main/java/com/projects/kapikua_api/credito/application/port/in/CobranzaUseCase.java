package com.projects.kapikua_api.credito.application.port.in;

import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.UsuarioId;

import java.time.LocalDate;

/** RN-22 a RN-26 y CU-13: retención de ventas y cobro diario automático. */
public interface CobranzaUseCase {

    /**
     * Ante cada cobro que recibe un usuario (pago a negocio o transferencia), dentro de la misma transacción:
     * si tiene un préstamo, aparta el porcentaje autorizado para su cuota; si es aval ACTIVADO, retiene de su venta
     * para las cuotas vencidas de quien respalda. Nunca impide el cobro.
     */
    void aplicarRetencion(UsuarioId usuarioId, Long transaccionVentaId, Dinero cobro);

    /**
     * CU-13: vence ofertas e invitaciones, cobra las cuotas que vencen {@code hoy} y las vencidas, calcula la mora,
     * escala al aval y cierra los préstamos pagados. Cada préstamo se procesa en su propia transacción: un error
     * en uno no detiene a los demás. Repetirlo el mismo día no cobra dos veces.
     */
    ResumenCobro ejecutarCobroDiario(LocalDate hoy);

    record ResumenCobro(int ofertasActualizadas, int prestamosRevisados, int cuotasCobradas, Dinero totalCobrado,
                        int prestamosEnMora, int avisosAval, int avalesActivados, int prestamosPagados, int errores) {}
}
