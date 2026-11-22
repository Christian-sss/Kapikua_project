package com.projects.kapikua_api.credito.repository;

import com.projects.kapikua_api.credito.entity.PrestamoEntity;
import com.projects.kapikua_api.credito.mapper.PrestamoMapper;
import com.projects.kapikua_api.credito.model.Prestamo;
import com.projects.kapikua_api.shared.UsuarioId;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Lee y guarda préstamos con sus cuotas, pagos y retenciones. Convierte con PrestamoMapper. */
@Repository
public class PrestamoRepository {
    private static final List<String> ACTIVOS = List.of("VIGENTE", "EN_MORA");

    private final PrestamoJpaRepository repositorio;
    private final EntityManager em;
    private final JdbcClient jdbc;

    public PrestamoRepository(PrestamoJpaRepository repositorio, EntityManager em, JdbcClient jdbc) {
        this.repositorio = repositorio;
        this.em = em;
        this.jdbc = jdbc;
    }

    public Optional<Prestamo> porId(Long prestamoId) {
        return repositorio.findById(prestamoId).map(this::aModelo);
    }

    public Optional<Prestamo> activoDe(UsuarioId usuarioId) {
        return repositorio.findFirstByUsuarioIdAndEstadoIn(usuarioId.valor(), ACTIVOS).map(this::aModelo);
    }

    public boolean tienePrestamoEnMora(UsuarioId usuarioId) {
        return repositorio.existsByUsuarioIdAndEstado(usuarioId.valor(), "EN_MORA");
    }

    public Prestamo registrar(Prestamo prestamo) {
        if (prestamo.getId() != null) throw new IllegalArgumentException("El préstamo ya está registrado");
        var fila = PrestamoMapper.aEntidadNueva(prestamo);
        em.persist(fila);
        em.flush();
        return PrestamoMapper.aModelo(fila, false);
    }

    /**
     * Copia los cambios del préstamo y sus cuotas, inserta los pagos y retenciones nuevos, y hace flush en el
     * momento para que un conflicto de versión salte aquí.
     */
    public Prestamo guardar(Prestamo prestamo) {
        var fila = repositorio.findById(prestamo.getId()).orElseThrow();
        if (!Objects.equals(fila.getVersion(), prestamo.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(PrestamoEntity.class, prestamo.getId());
        }
        PrestamoMapper.copiarCambios(prestamo, fila);
        prestamo.retencionesNuevas().forEach(r -> em.persist(PrestamoMapper.aEntidad(r)));
        prestamo.pagosNuevos().forEach(p -> em.persist(PrestamoMapper.aEntidad(prestamo.getId(), p)));
        em.flush();
        return PrestamoMapper.aModelo(fila, prestamo.isConPagosDeAval());
    }

    public List<Long> idsConCobroPendiente(LocalDate hoy) {
        return jdbc.sql("""
                        select distinct p.id from credito.prestamo p
                        join credito.cuota c on c.prestamo_id = p.id
                        where p.estado in ('VIGENTE', 'EN_MORA') and c.estado in ('PENDIENTE', 'VENCIDA')
                          and c.fecha_vencimiento <= :hoy
                        order by p.id
                        """)
                .param("hoy", hoy)
                .query(Long.class)
                .list();
    }

    public List<Long> idsConAvalActivadoDe(UsuarioId avalId) {
        return idsRespaldados(avalId, List.of("ACTIVADO"));
    }

    public List<Long> idsQueRespalda(UsuarioId avalId) {
        return idsRespaldados(avalId, List.of("ACEPTADO", "ACTIVADO"));
    }

    private List<Long> idsRespaldados(UsuarioId avalId, List<String> estadosAval) {
        return jdbc.sql("""
                        select p.id from credito.prestamo p
                        join credito.aval a on a.oferta_id = p.oferta_id
                        where a.usuario_aval_id = :aval and a.estado in (:estados) and p.estado in ('VIGENTE', 'EN_MORA')
                        order by p.id
                        """)
                .param("aval", avalId.valor())
                .param("estados", estadosAval)
                .query(Long.class)
                .list();
    }

    public boolean ventaYaRetenida(Long transaccionVentaId) {
        return jdbc.sql("select exists (select 1 from credito.retencion where transaccion_id = :tx)")
                .param("tx", transaccionVentaId)
                .query(Boolean.class)
                .single();
    }

    public boolean pagadoPorAval(Long prestamoId) {
        return jdbc.sql("select exists (select 1 from credito.pago where prestamo_id = :p and origen = 'AVAL')")
                .param("p", prestamoId)
                .query(Boolean.class)
                .single();
    }

    private Prestamo aModelo(PrestamoEntity fila) {
        return PrestamoMapper.aModelo(fila, pagadoPorAval(fila.getId()));
    }
}
