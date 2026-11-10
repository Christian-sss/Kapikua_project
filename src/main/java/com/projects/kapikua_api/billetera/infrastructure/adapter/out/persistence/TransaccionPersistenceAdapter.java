package com.projects.kapikua_api.billetera.infrastructure.adapter.out.persistence;

import com.projects.kapikua_api.shared.infrastructure.HoraLocal;
import com.projects.kapikua_api.billetera.application.port.out.TransaccionRepositoryPort;
import com.projects.kapikua_api.billetera.domain.model.AcumuladoMensual;
import com.projects.kapikua_api.billetera.domain.model.TipoTransaccion;
import com.projects.kapikua_api.billetera.domain.model.Transaccion;
import com.projects.kapikua_api.shared.Dinero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
class TransaccionPersistenceAdapter implements TransaccionRepositoryPort {
    private static final List<String> PAGOS_Y_TRANSFERENCIAS =
            List.of(TipoTransaccion.TRANSFERENCIA.name(), TipoTransaccion.PAGO_NEGOCIO.name());

    private final TransaccionJpaRepository repositorio;
    private final JdbcClient jdbc;

    TransaccionPersistenceAdapter(TransaccionJpaRepository repositorio, JdbcClient jdbc) {
        this.repositorio = repositorio;
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Transaccion> porReferencia(String referencia) {
        return repositorio.findByReferencia(referencia).map(TransaccionJpa::aDominio);
    }

    @Override
    public Optional<Transaccion> porId(Long transaccionId) {
        return repositorio.findById(transaccionId).map(TransaccionJpa::aDominio);
    }

    @Override
    public Transaccion registrar(Transaccion transaccion) {
        return repositorio.saveAndFlush(TransaccionJpa.desde(transaccion)).aDominio();
    }

    @Override
    public AcumuladoMensual acumulado(Long billeteraId, OffsetDateTime desde, OffsetDateTime hasta) {
        return jdbc.sql("""
                        select coalesce(sum(m.monto) filter (where t.tipo = 'RECARGA' and m.monto > 0), 0)     as recargas,
                               coalesce(-sum(m.monto) filter (where t.tipo in (:pagos) and m.monto < 0), 0)    as enviados
                        from billetera.movimiento m
                        join billetera.transaccion t on t.id = m.transaccion_id
                        where m.billetera_id = :billetera and m.fecha >= :desde and m.fecha < :hasta
                        """)
                .param("pagos", PAGOS_Y_TRANSFERENCIAS)
                .param("billetera", billeteraId)
                .param("desde", desde)
                .param("hasta", hasta)
                .query((rs, n) -> new AcumuladoMensual(Dinero.de(rs.getBigDecimal("recargas")),
                        Dinero.de(rs.getBigDecimal("enviados"))))
                .single();
    }

    @Override
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
                .query((rs, n) -> new LineaMovimiento(rs.getLong("id"), TipoTransaccion.valueOf(rs.getString("tipo")),
                        rs.getString("descripcion"), rs.getBigDecimal("monto"), rs.getBigDecimal("saldo_posterior"),
                        HoraLocal.leer(rs, "fecha")))
                .list();
    }

    @Override
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
                .param("tipos", PAGOS_Y_TRANSFERENCIAS)
                .param("desde", desde)
                .param("hasta", hasta)
                .query((rs, n) -> new LineaCobro(HoraLocal.leer(rs, "fecha"), rs.getBigDecimal("monto"),
                        rs.getLong("pagadora")))
                .list();
    }
}

interface TransaccionJpaRepository extends JpaRepository<TransaccionJpa, Long> {
    Optional<TransaccionJpa> findByReferencia(String referencia);
}
