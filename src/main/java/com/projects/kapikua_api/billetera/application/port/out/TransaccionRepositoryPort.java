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
    Optional<Transaccion> porReferencia(String referencia);

    Optional<Transaccion> porId(Long transaccionId);

    Transaccion registrar(Transaccion transaccion);

    AcumuladoMensual acumulado(Long billeteraId, OffsetDateTime desde, OffsetDateTime hasta);

    List<LineaMovimiento> movimientos(Long billeteraId, OffsetDateTime desde, OffsetDateTime hasta, int limite);

    List<LineaCobro> cobrosRecibidos(Long billeteraId, OffsetDateTime desde, OffsetDateTime hasta);

    record LineaCobro(OffsetDateTime fecha, BigDecimal monto, Long billeteraPagadora) {}

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
