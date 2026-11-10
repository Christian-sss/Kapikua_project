package com.projects.kapikua_api.billetera.application.port.out;

import com.projects.kapikua_api.billetera.domain.model.AcumuladoMensual;
import com.projects.kapikua_api.billetera.domain.model.TipoTransaccion;
import com.projects.kapikua_api.billetera.domain.model.Transaccion;
import com.projects.kapikua_api.shared.Dinero;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface TransaccionRepositoryPort {

    /** Idempotencia (RNF-07): la referencia es única. */
    Optional<Transaccion> porReferencia(String referencia);

    Optional<Transaccion> porId(Long transaccionId);

    Transaccion registrar(Transaccion transaccion);

    /** Recargas y pagos o transferencias enviados entre {@code desde} (incluido) y {@code hasta} (excluido). */
    AcumuladoMensual acumulado(Long billeteraId, OffsetDateTime desde, OffsetDateTime hasta);

    /** Movimientos de la billetera, del más reciente al más antiguo. */
    List<LineaMovimiento> movimientos(Long billeteraId, OffsetDateTime desde, OffsetDateTime hasta, int limite);

    /**
     * Cobros recibidos: pagos a negocio y transferencias que entraron a la billetera, con quién pagó.
     * No incluye recargas ni desembolsos (RN-08).
     */
    List<LineaCobro> cobrosRecibidos(Long billeteraId, OffsetDateTime desde, OffsetDateTime hasta);

    record LineaCobro(OffsetDateTime fecha, BigDecimal monto, Long billeteraPagadora) {}

    /** {@code monto}: positivo entra, negativo sale. */
    record LineaMovimiento(Long transaccionId, TipoTransaccion tipo, String descripcion, BigDecimal monto,
                           BigDecimal saldoPosterior, OffsetDateTime fecha) {

        public boolean esEntrada() {
            return monto.signum() > 0;
        }

        public Dinero montoAbsoluto() {
            return Dinero.de(monto.abs());
        }
    }
}
