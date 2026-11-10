package com.projects.kapikua_api.credito.infrastructure.adapter.out.persistence;

import com.projects.kapikua_api.credito.application.port.out.OfertaRepositoryPort;
import com.projects.kapikua_api.credito.domain.model.Aval;
import com.projects.kapikua_api.credito.domain.model.CondicionesCredito;
import com.projects.kapikua_api.credito.domain.model.EstadoAval;
import com.projects.kapikua_api.credito.domain.model.EstadoOferta;
import com.projects.kapikua_api.credito.domain.model.Oferta;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.UsuarioId;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
class OfertaPersistenceAdapter implements OfertaRepositoryPort {
    private static final List<String> GARANTIA_ACTIVA = nombres(EstadoAval.ACEPTADO, EstadoAval.ACTIVADO);
    private static final List<String> EN_CURSO = nombres(EstadoAval.PENDIENTE, EstadoAval.ACEPTADO, EstadoAval.ACTIVADO);

    private final OfertaJpaRepository repositorio;
    private final EntityManager em;
    private final JdbcClient jdbc;

    OfertaPersistenceAdapter(OfertaJpaRepository repositorio, EntityManager em, JdbcClient jdbc) {
        this.repositorio = repositorio;
        this.em = em;
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Oferta> porId(Long ofertaId) {
        return repositorio.findById(ofertaId).map(OfertaJpa::aDominio);
    }

    @Override
    public Optional<Oferta> ultimaDe(UsuarioId usuarioId) {
        return repositorio.findFirstByUsuarioIdOrderByIdDesc(usuarioId.valor()).map(OfertaJpa::aDominio);
    }

    @Override
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
                .sorted(Comparator.comparing(o -> o.id))
                .map(OfertaJpa::aDominio).toList();
    }

    @Override
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

    @Override
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

    @Override
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

    private static List<String> nombres(EstadoAval... estados) {
        return Arrays.stream(estados).map(Enum::name).toList();
    }

    @Override
    public Oferta guardar(Oferta o) {
        if (o.getId() == null) {
            var nueva = new OfertaJpa();
            nueva.evaluacionId = o.getEvaluacionId();
            nueva.usuarioId = o.getUsuarioId().valor();
            nueva.fecha = o.getFecha();
            nueva.vigenteHasta = o.getVigenteHasta();
            copiar(o, nueva);
            em.persist(nueva);
            em.flush();
            return nueva.aDominio();
        }
        var j = repositorio.findById(o.getId()).orElseThrow();
        if (!Objects.equals(j.version, o.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(OfertaJpa.class, o.getId());
        }
        copiar(o, j);
        em.flush();
        return j.aDominio();
    }

    private static void copiar(Oferta o, OfertaJpa j) {
        j.codigoSolicitudEfa = o.getCodigoSolicitudEfa();
        j.estado = o.getEstado();
        if (o.getCondiciones() != null) {
            var c = o.getCondiciones();
            j.monto = c.monto().valor();
            j.numeroCuotas = c.cuotas();
            j.tea = c.tea();
            j.tcea = c.tcea();
            j.montoCuota = c.cuota().valor();
        }
        j.motivoDenegacion = o.getMotivoDenegacion();
        for (var a : o.getAvales()) {
            var existente = a.getId() == null ? null
                    : j.avales.stream().filter(x -> x.id.equals(a.getId())).findFirst().orElse(null);
            var aj = existente != null ? existente : new AvalJpa();
            if (existente == null) {
                aj.usuarioDeudorId = a.getUsuarioDeudorId().valor();
                aj.usuarioAvalId = a.getUsuarioAvalId().valor();
                aj.fechaInvitacion = a.getFechaInvitacion();
                j.avales.add(aj);
            }
            aj.estado = a.getEstado();
            aj.porcentajeRetencionAutorizado = a.getPorcentajeRetencionAutorizado() == null ? null
                    : BigDecimal.valueOf(a.getPorcentajeRetencionAutorizado());
            aj.fechaRespuesta = a.getFechaRespuesta();
            aj.dispositivoAceptacion = a.getDispositivoAceptacion();
            aj.fechaAviso = a.getFechaAviso();
            aj.fechaActivacion = a.getFechaActivacion();
            aj.fechaLiberacion = a.getFechaLiberacion();
        }
    }
}

interface OfertaJpaRepository extends JpaRepository<OfertaJpa, Long> {
    Optional<OfertaJpa> findFirstByUsuarioIdOrderByIdDesc(Long usuarioId);
}

@Entity
@Table(schema = "credito", name = "oferta")
class OfertaJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "evaluacion_id", nullable = false, unique = true)
    Long evaluacionId;

    @Column(name = "usuario_id", nullable = false)
    Long usuarioId;

    @Column(name = "codigo_solicitud_efa", unique = true, length = 30)
    String codigoSolicitudEfa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    EstadoOferta estado;

    @Column(precision = 12, scale = 2)
    BigDecimal monto;

    @Column(name = "numero_cuotas")
    Integer numeroCuotas;

    @Column(precision = 6, scale = 2)
    BigDecimal tea;

    @Column(precision = 6, scale = 2)
    BigDecimal tcea;

    @Column(name = "monto_cuota", precision = 12, scale = 2)
    BigDecimal montoCuota;

    @Column(name = "motivo_denegacion", length = 255)
    String motivoDenegacion;

    @Column(nullable = false)
    OffsetDateTime fecha;

    @Column(name = "vigente_hasta", nullable = false)
    LocalDate vigenteHasta;

    @Version
    @Column(nullable = false)
    Long version;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "oferta_id", nullable = false, updatable = false)
    @OrderBy("id")
    List<AvalJpa> avales = new ArrayList<>();

    Oferta aDominio() {
        var condiciones = monto == null ? null : new CondicionesCredito(Dinero.de(monto), numeroCuotas, tea, tcea,
                Dinero.de(montoCuota));
        return Oferta.reconstituir(id, evaluacionId, new UsuarioId(usuarioId), codigoSolicitudEfa, estado, condiciones,
                motivoDenegacion, fecha, vigenteHasta, version, avales.stream().map(AvalJpa::aDominio).toList());
    }
}

@Entity
@Table(schema = "credito", name = "aval")
class AvalJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "usuario_deudor_id", nullable = false)
    Long usuarioDeudorId;

    @Column(name = "usuario_aval_id", nullable = false)
    Long usuarioAvalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    EstadoAval estado;

    @Column(name = "porcentaje_retencion_autorizado", precision = 5, scale = 2)
    BigDecimal porcentajeRetencionAutorizado;

    @Column(name = "fecha_invitacion", nullable = false)
    OffsetDateTime fechaInvitacion;

    @Column(name = "fecha_respuesta")
    OffsetDateTime fechaRespuesta;

    @Column(name = "dispositivo_aceptacion", length = 150)
    String dispositivoAceptacion;

    @Column(name = "fecha_aviso")
    OffsetDateTime fechaAviso;

    @Column(name = "fecha_activacion")
    OffsetDateTime fechaActivacion;

    @Column(name = "fecha_liberacion")
    OffsetDateTime fechaLiberacion;

    @Version
    @Column(nullable = false)
    Long version;

    Aval aDominio() {
        return Aval.reconstituir(id, new UsuarioId(usuarioDeudorId), new UsuarioId(usuarioAvalId), estado,
                porcentajeRetencionAutorizado == null ? null : porcentajeRetencionAutorizado.intValue(),
                fechaInvitacion, fechaRespuesta, dispositivoAceptacion, fechaAviso, fechaActivacion, fechaLiberacion);
    }
}
