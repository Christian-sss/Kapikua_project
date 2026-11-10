package com.projects.kapikua_api.credito.infrastructure.adapter.out.persistence;

import com.projects.kapikua_api.credito.application.port.out.PrestamoRepositoryPort;
import com.projects.kapikua_api.credito.domain.model.ConstanciaAceptacion;
import com.projects.kapikua_api.credito.domain.model.Cuota;
import com.projects.kapikua_api.credito.domain.model.EstadoCuota;
import com.projects.kapikua_api.credito.domain.model.EstadoPrestamo;
import com.projects.kapikua_api.credito.domain.model.ModalidadPago;
import com.projects.kapikua_api.credito.domain.model.Origen;
import com.projects.kapikua_api.credito.domain.model.Prestamo;
import com.projects.kapikua_api.credito.domain.model.TipoPago;
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
import org.hibernate.annotations.Immutable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * El préstamo y sus cuotas se leen y guardan con JPA (bloqueo optimista); las retenciones y los pagos se insertan
 * (son de solo inserción); las consultas de la cobranza van con SQL sobre JdbcClient.
 */
@Repository
class PrestamoPersistenceAdapter implements PrestamoRepositoryPort {

    private static final List<EstadoPrestamo> ACTIVOS = List.of(EstadoPrestamo.VIGENTE, EstadoPrestamo.EN_MORA);

    private final PrestamoJpaRepository repositorio;
    private final EntityManager em;
    private final JdbcClient jdbc;

    PrestamoPersistenceAdapter(PrestamoJpaRepository repositorio, EntityManager em, JdbcClient jdbc) {
        this.repositorio = repositorio;
        this.em = em;
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Prestamo> porId(Long prestamoId) {
        return repositorio.findById(prestamoId).map(this::aDominio);
    }

    @Override
    public Optional<Prestamo> activoDe(UsuarioId usuarioId) {
        return repositorio.findFirstByUsuarioIdAndEstadoIn(usuarioId.valor(), ACTIVOS).map(this::aDominio);
    }

    @Override
    public boolean tienePrestamoEnMora(UsuarioId usuarioId) {
        return repositorio.existsByUsuarioIdAndEstado(usuarioId.valor(), EstadoPrestamo.EN_MORA);
    }

    @Override
    public Prestamo registrar(Prestamo p) {
        if (p.getId() != null) throw new IllegalArgumentException("El préstamo ya está registrado");
        var j = new PrestamoJpa();
        j.ofertaId = p.getOfertaId();
        j.usuarioId = p.getUsuarioId().valor();
        j.codigoEntidad = p.getCodigoEntidad();
        j.codigoCreditoEfa = p.getCodigoCreditoEfa();
        j.monto = p.getMonto().valor();
        j.tea = p.getTea();
        j.tcea = p.getTcea();
        j.porcentajeRetencion = BigDecimal.valueOf(p.getPorcentajeRetencion());
        j.versionHojaResumen = p.getConstancia().versionHojaResumen();
        j.fechaAceptacion = p.getConstancia().fecha();
        j.dispositivoAceptacion = p.getConstancia().dispositivo();
        j.fechaDesembolso = p.getFechaDesembolso();
        j.fechaAutorizacionCargo = p.getFechaAutorizacionCargo();
        copiar(p, j);
        for (var c : p.getCuotas()) {
            var cj = new CuotaJpa();
            cj.numero = c.getNumero();
            cj.fechaVencimiento = c.getFechaVencimiento();
            copiar(c, cj);
            j.cuotas.add(cj);
        }
        em.persist(j);
        em.flush();
        return j.aDominio(false);
    }

    /**
     * Copia el estado del préstamo y de sus cuotas sobre las entidades gestionadas, con control de versión,
     * e inserta las retenciones y los pagos nuevos.
     */
    @Override
    public Prestamo guardar(Prestamo p) {
        var j = repositorio.findById(p.getId()).orElseThrow();
        if (!Objects.equals(j.version, p.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(PrestamoJpa.class, p.getId());
        }
        copiar(p, j);
        for (var c : p.getCuotas()) {
            var cj = j.cuotas.stream().filter(x -> x.id.equals(c.getId())).findFirst().orElseThrow();
            copiar(c, cj);
        }
        for (var r : p.retencionesNuevas()) {
            var rj = new RetencionJpa();
            rj.cuotaId = r.cuotaId();
            rj.transaccionId = r.transaccionVentaId();
            rj.origen = r.origen();
            rj.monto = r.monto().valor();
            rj.fecha = r.fecha();
            em.persist(rj);
        }
        for (var pago : p.pagosNuevos()) {
            var pj = new PagoJpa();
            pj.prestamoId = p.getId();
            pj.cuotaId = pago.cuotaId();
            pj.transaccionId = pago.transaccionId();
            pj.tipo = pago.tipo();
            pj.origen = pago.origen();
            pj.automatico = pago.automatico();
            pj.modalidad = pago.modalidad();
            pj.monto = pago.monto().valor();
            pj.imputadoMoratorio = pago.imputacion().moratorio().valor();
            pj.imputadoInteres = pago.imputacion().interes().valor();
            pj.imputadoCapital = pago.imputacion().capital().valor();
            pj.fecha = pago.fecha();
            em.persist(pj);
        }
        em.flush();
        return j.aDominio(p.isConPagosDeAval());
    }

    private static void copiar(Prestamo p, PrestamoJpa j) {
        j.cargoAutorizado = p.isCargoAutorizado();
        j.fechaRevocacionCargo = p.getFechaRevocacionCargo();
        j.estado = p.getEstado();
    }

    private static void copiar(Cuota c, CuotaJpa cj) {
        cj.capital = c.getCapital().valor();
        cj.interes = c.getInteres().valor();
        cj.interesMoratorio = c.getInteresMoratorio().valor();
        cj.montoPagado = c.getMontoPagado().valor();
        cj.montoApartado = c.getMontoApartado().valor();
        cj.fechaPago = c.getFechaPago();
        cj.estado = c.getEstado();
    }

    // ------------------------------------------------------------------ consultas de la cobranza

    @Override
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

    @Override
    public List<Long> idsConAvalActivadoDe(UsuarioId avalId) {
        return idsRespaldados(avalId, List.of("ACTIVADO"));
    }

    @Override
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

    @Override
    public boolean ventaYaRetenida(Long transaccionVentaId) {
        return jdbc.sql("select exists (select 1 from credito.retencion where transaccion_id = :tx)")
                .param("tx", transaccionVentaId)
                .query(Boolean.class)
                .single();
    }

    private Prestamo aDominio(PrestamoJpa j) {
        return j.aDominio(pagadoPorAval(j.id));
    }

    @Override
    public boolean pagadoPorAval(Long prestamoId) {
        return jdbc.sql("select exists (select 1 from credito.pago where prestamo_id = :p and origen = 'AVAL')")
                .param("p", prestamoId)
                .query(Boolean.class)
                .single();
    }
}

interface PrestamoJpaRepository extends JpaRepository<PrestamoJpa, Long> {

    Optional<PrestamoJpa> findFirstByUsuarioIdAndEstadoIn(Long usuarioId, Collection<EstadoPrestamo> estados);

    boolean existsByUsuarioIdAndEstado(Long usuarioId, EstadoPrestamo estado);
}

/** credito.retencion: solo inserción (trigger trg_retencion_inmutable). */
@Entity
@Immutable
@Table(schema = "credito", name = "retencion")
class RetencionJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "cuota_id", nullable = false)
    Long cuotaId;

    @Column(name = "transaccion_id", nullable = false, unique = true)
    Long transaccionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 7)
    Origen origen;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal monto;

    @Column(nullable = false)
    OffsetDateTime fecha;
}

/** credito.pago: solo inserción (trigger trg_pago_inmutable). */
@Entity
@Immutable
@Table(schema = "credito", name = "pago")
class PagoJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "prestamo_id", nullable = false)
    Long prestamoId;

    @Column(name = "cuota_id")
    Long cuotaId;

    @Column(name = "transaccion_id", nullable = false)
    Long transaccionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    TipoPago tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 7)
    Origen origen;

    @Column(nullable = false)
    boolean automatico;

    @Enumerated(EnumType.STRING)
    @Column(length = 13)
    ModalidadPago modalidad;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal monto;

    @Column(name = "imputado_moratorio", nullable = false, precision = 12, scale = 2)
    BigDecimal imputadoMoratorio;

    @Column(name = "imputado_interes", nullable = false, precision = 12, scale = 2)
    BigDecimal imputadoInteres;

    @Column(name = "imputado_capital", nullable = false, precision = 12, scale = 2)
    BigDecimal imputadoCapital;

    @Column(nullable = false)
    OffsetDateTime fecha;
}

@Entity
@Table(schema = "credito", name = "prestamo")
class PrestamoJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "oferta_id", nullable = false, unique = true)
    Long ofertaId;

    @Column(name = "usuario_id", nullable = false)
    Long usuarioId;

    @Column(name = "codigo_entidad", nullable = false, length = 20)
    String codigoEntidad;

    @Column(name = "codigo_credito_efa", nullable = false, unique = true, length = 30)
    String codigoCreditoEfa;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal monto;

    @Column(nullable = false, precision = 6, scale = 2)
    BigDecimal tea;

    @Column(nullable = false, precision = 6, scale = 2)
    BigDecimal tcea;

    @Column(name = "porcentaje_retencion", nullable = false, precision = 5, scale = 2)
    BigDecimal porcentajeRetencion;

    @Column(name = "version_hoja_resumen", nullable = false, length = 20)
    String versionHojaResumen;

    @Column(name = "fecha_aceptacion", nullable = false)
    OffsetDateTime fechaAceptacion;

    @Column(name = "dispositivo_aceptacion", nullable = false, length = 150)
    String dispositivoAceptacion;

    @Column(name = "cargo_autorizado", nullable = false)
    boolean cargoAutorizado;

    @Column(name = "fecha_autorizacion_cargo")
    OffsetDateTime fechaAutorizacionCargo;

    @Column(name = "fecha_revocacion_cargo")
    OffsetDateTime fechaRevocacionCargo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    EstadoPrestamo estado;

    @Column(name = "fecha_desembolso", nullable = false)
    OffsetDateTime fechaDesembolso;

    @Version
    @Column(nullable = false)
    Long version;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "prestamo_id", nullable = false, updatable = false)
    @OrderBy("numero")
    List<CuotaJpa> cuotas = new ArrayList<>();

    Prestamo aDominio(boolean conPagosDeAval) {
        return Prestamo.reconstituir(id, ofertaId, new UsuarioId(usuarioId), codigoEntidad, codigoCreditoEfa,
                Dinero.de(monto), tea, tcea, porcentajeRetencion.intValue(),
                new ConstanciaAceptacion(fechaAceptacion, dispositivoAceptacion, versionHojaResumen), cargoAutorizado,
                fechaAutorizacionCargo, fechaRevocacionCargo, estado, fechaDesembolso, version,
                cuotas.stream().map(CuotaJpa::aDominio).toList(), conPagosDeAval);
    }
}

@Entity
@Table(schema = "credito", name = "cuota")
class CuotaJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    Integer numero;

    @Column(name = "fecha_vencimiento", nullable = false)
    LocalDate fechaVencimiento;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal capital;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal interes;

    @Column(name = "interes_moratorio", nullable = false, precision = 12, scale = 2)
    BigDecimal interesMoratorio;

    @Column(name = "monto_pagado", nullable = false, precision = 12, scale = 2)
    BigDecimal montoPagado;

    @Column(name = "monto_apartado", nullable = false, precision = 12, scale = 2)
    BigDecimal montoApartado;

    @Column(name = "fecha_pago")
    LocalDate fechaPago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    EstadoCuota estado;

    Cuota aDominio() {
        return Cuota.reconstituir(id, numero, fechaVencimiento, Dinero.de(capital), Dinero.de(interes),
                Dinero.de(interesMoratorio), Dinero.de(montoPagado), Dinero.de(montoApartado), fechaPago, estado);
    }
}
