package com.projects.kapikua_api.credito.repository;

import com.projects.kapikua_api.credito.entity.OfertaEntity;
import com.projects.kapikua_api.credito.mapper.OfertaMapper;
import com.projects.kapikua_api.credito.model.Oferta;
import com.projects.kapikua_api.shared.UsuarioId;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Lee y guarda ofertas con sus avales. Convierte entre las filas y el modelo con OfertaMapper. */
@Repository
public class OfertaRepository {
    private static final List<String> GARANTIA_ACTIVA = List.of("ACEPTADO", "ACTIVADO");
    private static final List<String> EN_CURSO = List.of("PENDIENTE", "ACEPTADO", "ACTIVADO");

    private final OfertaJpaRepository repositorio;
    private final EntityManager em;
    private final JdbcClient jdbc;

    public OfertaRepository(OfertaJpaRepository repositorio, EntityManager em, JdbcClient jdbc) {
        this.repositorio = repositorio;
        this.em = em;
        this.jdbc = jdbc;
    }

    public Optional<Oferta> porId(Long ofertaId) {
        return repositorio.findById(ofertaId).map(OfertaMapper::aModelo);
    }

    public Optional<Oferta> ultimaDe(UsuarioId usuarioId) {
        return repositorio.findFirstByUsuarioIdOrderByIdDesc(usuarioId.valor()).map(OfertaMapper::aModelo);
    }

    public List<Oferta> conInvitacionPendientePara(UsuarioId avalId) {
        var ids = jdbc.sql("""
                        select distinct oferta_id from credito.aval
                        where usuario_aval_id = :aval and estado = 'PENDIENTE'
                        order by oferta_id
                        """)
                .param("aval", avalId.valor())
                .query(Long.class)
                .list();
        return repositorio.findAllById(ids).stream()
                .sorted(Comparator.comparing(OfertaEntity::getId))
                .map(OfertaMapper::aModelo).toList();
    }

    public boolean tieneGarantiaActiva(UsuarioId avalId) {
        return jdbc.sql("""
                        select exists (select 1 from credito.aval
                                       where usuario_aval_id = :aval and estado in (:estados))
                        """)
                .param("aval", avalId.valor())
                .param("estados", GARANTIA_ACTIVA)
                .query(Boolean.class)
                .single();
    }

    public boolean esAvalEnCursoDe(UsuarioId avalDe, UsuarioId deudor) {
        return jdbc.sql("""
                        select exists (select 1 from credito.aval
                                       where usuario_aval_id = :aval and usuario_deudor_id = :deudor
                                         and estado in (:estados))
                        """)
                .param("aval", avalDe.valor())
                .param("deudor", deudor.valor())
                .param("estados", EN_CURSO)
                .query(Boolean.class)
                .single();
    }

    public List<Long> idsConVencimientos(LocalDate hoy, OffsetDateTime invitadasAntesDe) {
        return jdbc.sql("""
                        select o.id from credito.oferta o
                        where o.estado in ('PENDIENTE_EFA', 'CONFIRMADA') and o.vigente_hasta < :hoy
                        union
                        select a.oferta_id from credito.aval a
                        where a.estado = 'PENDIENTE' and a.fecha_invitacion <= :limite
                        order by 1
                        """)
                .param("hoy", hoy)
                .param("limite", invitadasAntesDe)
                .query(Long.class)
                .list();
    }

    /**
     * Inserta la oferta nueva o copia sus cambios a las filas, y hace flush en el momento para que un conflicto de
     * versión salte aquí.
     */
    public Oferta guardar(Oferta oferta) {
        if (oferta.getId() == null) {
            var nueva = OfertaMapper.aEntidadNueva(oferta);
            em.persist(nueva);
            em.flush();
            return OfertaMapper.aModelo(nueva);
        }
        var fila = repositorio.findById(oferta.getId()).orElseThrow();
        if (!Objects.equals(fila.getVersion(), oferta.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(OfertaEntity.class, oferta.getId());
        }
        OfertaMapper.copiarCambios(oferta, fila);
        em.flush();
        return OfertaMapper.aModelo(fila);
    }
}
