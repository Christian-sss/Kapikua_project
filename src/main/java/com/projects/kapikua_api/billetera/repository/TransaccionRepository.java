package com.projects.kapikua_api.billetera.repository;

import com.projects.kapikua_api.shared.infrastructure.HoraLocal;
import com.projects.kapikua_api.billetera.dto.LineaCobro;
import com.projects.kapikua_api.billetera.dto.LineaMovimiento;
import com.projects.kapikua_api.billetera.mapper.TransaccionMapper;
import com.projects.kapikua_api.billetera.model.AcumuladoMensual;
import com.projects.kapikua_api.billetera.model.TipoTransaccion;
import com.projects.kapikua_api.billetera.model.TiposTransaccion;
import com.projects.kapikua_api.billetera.model.Transaccion;
import com.projects.kapikua_api.shared.Dinero;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** Guarda y lee transacciones (JPA + TransaccionMapper) y hace las consultas de saldos y cobros con SQL. */
@Repository
public class TransaccionRepository {
    private static final List<String> PAGOS_Y_TRANSFERENCIAS = TiposTransaccion.todos().stream()
            .filter(TipoTransaccion::cuentaComoPagoOTransferencia)
            .map(TipoTransaccion::codigo)
            .toList();
    private static final List<String> COBROS_RECIBIDOS = TiposTransaccion.todos().stream()
            .filter(TipoTransaccion::generaCobroRecibido)
            .map(TipoTransaccion::codigo)
            .toList();

    private final TransaccionJpaRepository repositorio;
    private final JdbcClient jdbc;

    public TransaccionRepository(TransaccionJpaRepository repositorio, JdbcClient jdbc) {
        this.repositorio = repositorio;
        this.jdbc = jdbc;
    }

    public Optional<Transaccion> porReferencia(String referencia) {
        return repositorio.findByReferencia(referencia).map(TransaccionMapper::aModelo);
    }

    public Optional<Transaccion> porId(Long transaccionId) {
        return repositorio.findById(transaccionId).map(TransaccionMapper::aModelo);
    }

    public Transaccion registrar(Transaccion transaccion) {
        return TransaccionMapper.aModelo(repositorio.saveAndFlush(TransaccionMapper.aEntidad(transaccion)));
    }

    public AcumuladoMensual acumulado(Long billeteraId, OffsetDateTime desde, OffsetDateTime hasta) {
        return jdbc.sql("""
                        select coalesce(-sum(m.monto) filter (where t.tipo in (:pagos) and m.monto < 0), 0) as enviados
                        from billetera.movimiento m
                        join billetera.transaccion t on t.id = m.transaccion_id
                        where m.billetera_id = :billetera and m.fecha >= :desde and m.fecha < :hasta
                        """)
                .param("pagos", PAGOS_Y_TRANSFERENCIAS)
                .param("billetera", billeteraId)
                .param("desde", desde)
                .param("hasta", hasta)
                .query((rs, n) -> new AcumuladoMensual(Dinero.de(rs.getBigDecimal("enviados"))))
                .single();
    }

    public List<LineaMovimiento> movimientos(Long billeteraId, OffsetDateTime desde, OffsetDateTime hasta, int limite) {
        return jdbc.sql("""
                        select t.id, t.tipo, t.descripcion, m.monto, m.saldo_posterior, t.fecha
                        from billetera.movimiento m
                        join billetera.transaccion t on t.id = m.transaccion_id
                        where m.billetera_id = :billetera and m.fecha >= :desde and m.fecha < :hasta
                        order by m.fecha desc, m.id desc
                        limit :limite
                        """)
                .param("billetera", billeteraId)
                .param("desde", desde)
                .param("hasta", hasta)
                .param("limite", limite)
                .query((rs, n) -> new LineaMovimiento(rs.getLong("id"), TiposTransaccion.desdeCodigo(rs.getString("tipo")),
                        rs.getString("descripcion"), rs.getBigDecimal("monto"), rs.getBigDecimal("saldo_posterior"),
                        HoraLocal.leer(rs, "fecha")))
                .list();
    }

    public List<LineaCobro> cobrosRecibidos(Long billeteraId, OffsetDateTime desde, OffsetDateTime hasta) {
        return jdbc.sql("""
                        select entra.fecha, entra.monto, sale.billetera_id as pagadora
                        from billetera.movimiento entra
                        join billetera.transaccion t on t.id = entra.transaccion_id
                        join billetera.movimiento sale on sale.transaccion_id = t.id and sale.monto < 0
                        where entra.billetera_id = :billetera and entra.monto > 0 and t.tipo in (:tipos)
                          and entra.fecha >= :desde and entra.fecha < :hasta
                        """)
                .param("billetera", billeteraId)
                .param("tipos", COBROS_RECIBIDOS)
                .param("desde", desde)
                .param("hasta", hasta)
                .query((rs, n) -> new LineaCobro(HoraLocal.leer(rs, "fecha"), rs.getBigDecimal("monto"),
                        rs.getLong("pagadora")))
                .list();
    }
}
