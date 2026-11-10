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

public interface ConsultarBilleteraUseCase {
    Result<ResumenBilletera, ErrorBilletera> resumen(UsuarioId usuarioId, LocalDate desde, LocalDate hasta);

    boolean celularRegistrado(String celular);

    Optional<UsuarioId> titularDe(Long billeteraId);

    Optional<UsuarioId> usuarioPorCelular(String celular);

    Optional<ResumenCobros> cobros(UsuarioId usuarioId, LocalDate fechaSolicitud, int semanasPeriodo);

    record ResumenCobros(int semanas, Dinero totalCobrado, int semanasConCobros, int cantidadCobros,
                         int pagadoresDistintos, long diasAntiguedad) {
    }

    Optional<Comprobante> comprobante(UsuarioId usuarioId, Long transaccionId);

    List<OperacionTitular> operacionesDelTitular(Long billeteraId, OffsetDateTime desde, OffsetDateTime hasta);

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
