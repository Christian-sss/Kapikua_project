package com.projects.kapikua_api.billetera.application.port.in;

import com.projects.kapikua_api.billetera.application.port.out.TransaccionRepositoryPort.LineaMovimiento;
import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * CU-03 Consultar billetera (RF-BI-01). También responde consultas de otros contextos.
 */
public interface ConsultarBilleteraUseCase {

    /**
     * Historial entre {@code desde} y {@code hasta} (ambos incluidos); sin fechas, los últimos 30 días.
     */
    Result<ResumenBilletera, ErrorBilletera> resumen(UsuarioId usuarioId, LocalDate desde, LocalDate hasta);

    boolean celularRegistrado(String celular);

    Optional<UsuarioId> titularDe(Long billeteraId);

    /**
     * Para designar al aval por su celular (CU-09). Vacío si no hay billetera con ese número.
     */
    Optional<UsuarioId> usuarioPorCelular(String celular);

    /**
     * Para Evaluación (RN-08, RN-12): cobros de las últimas {@code semanasPeriodo} semanas completas
     * (lunes a domingo) antes de {@code fechaSolicitud}, o de las semanas completas desde la apertura
     * si la billetera es más nueva. Vacío si el usuario no tiene billetera.
     */
    Optional<ResumenCobros> cobros(UsuarioId usuarioId, LocalDate fechaSolicitud, int semanasPeriodo);

    record ResumenCobros(int semanas, Dinero totalCobrado, int semanasConCobros, int cantidadCobros,
                         int pagadoresDistintos, long diasAntiguedad) {
    }

    /**
     * El comprobante de una operación en la que participó el usuario; vacío si es ajena o no existe.
     */
    Optional<Comprobante> comprobante(UsuarioId usuarioId, Long transaccionId);

    /**
     * Para Cumplimiento (RN-28): las operaciones que hizo el titular entre {@code desde} y {@code hasta}
     * (recargas y envíos: transferencias y pagos a negocio), de la más antigua a la más reciente.
     */
    List<OperacionTitular> operacionesDelTitular(Long billeteraId, OffsetDateTime desde, OffsetDateTime hasta);

    /**
     * Para Cumplimiento: si la billetera está bloqueada.
     */
    boolean bloqueada(Long billeteraId);

    record OperacionTitular(Long transaccionId, String tipo, Dinero monto, OffsetDateTime fecha) {
    }

    record ResumenBilletera(String celular,
            String nombreNegocio,
            Dinero saldo,
            Dinero saldoApartado,
            Dinero saldoDisponible,
            Dinero recargasUsadas,
            Dinero recargasDisponibles,
            Dinero pagosUsados,
            Dinero pagosDisponibles,
            Dinero topePorOperacion,
            Dinero saldoMaximo,
            LocalDate desde,
            LocalDate hasta,
            List<LineaMovimiento> movimientos) {
}
}
