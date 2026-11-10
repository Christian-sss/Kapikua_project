package com.projects.kapikua_api.credito.application.port.out;

import com.projects.kapikua_api.credito.domain.model.Prestamo;
import com.projects.kapikua_api.shared.UsuarioId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PrestamoRepositoryPort {

    Optional<Prestamo> porId(Long prestamoId);

    /** RN-09: el préstamo VIGENTE o EN_MORA del cliente, si tiene. */
    Optional<Prestamo> activoDe(UsuarioId usuarioId);

    boolean tienePrestamoEnMora(UsuarioId usuarioId);

    Prestamo registrar(Prestamo prestamo);

    /** Con bloqueo optimista. Inserta las retenciones y pagos nuevos (son de solo inserción). */
    Prestamo guardar(Prestamo prestamo);

    /** CU-13: préstamos activos con alguna cuota por cobrar que vence {@code hoy} o antes. */
    List<Long> idsConCobroPendiente(LocalDate hoy);

    /** RN-25: préstamos activos cuyo aval ACTIVADO es este usuario (se retienen sus ventas). */
    List<Long> idsConAvalActivadoDe(UsuarioId avalId);

    /** CU-12, vista del aval: préstamos activos que este usuario respalda (aval ACEPTADO o ACTIVADO). */
    List<Long> idsQueRespalda(UsuarioId avalId);

    /** RN-22: cada venta se retiene una sola vez (credito.retencion.transaccion_id es único). */
    boolean ventaYaRetenida(Long transaccionVentaId);

    /** RN-32: el aval tuvo que pagar alguna cuota de este préstamo. */
    boolean pagadoPorAval(Long prestamoId);
}
