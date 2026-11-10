package com.projects.kapikua_api.evaluacion.infrastructure.adapter.out.persistence;

import com.projects.kapikua_api.evaluacion.application.port.out.HistorialRepositoryPort;
import com.projects.kapikua_api.evaluacion.domain.model.HistorialCrediticio;
import com.projects.kapikua_api.shared.UsuarioId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

@Repository
class HistorialPersistenceAdapter implements HistorialRepositoryPort {

    private final HistorialJpaRepository repositorio;
    private final EntityManager em;

    HistorialPersistenceAdapter(HistorialJpaRepository repositorio, EntityManager em) {
        this.repositorio = repositorio;
        this.em = em;
    }

    @Override
    public Optional<HistorialCrediticio> porUsuario(UsuarioId usuarioId) {
        return repositorio.findById(usuarioId.valor()).map(HistorialJpa::aDominio);
    }

    /** Igual que la billetera: si la versión leída ya no es la vigente, se lanza el conflicto (bloqueo optimista). */
    @Override
    public HistorialCrediticio guardar(HistorialCrediticio h) {
        var gestionado = repositorio.findById(h.getUsuarioId().valor());
        if (gestionado.isEmpty()) {
            if (h.getVersion() != null) {
                throw new ObjectOptimisticLockingFailureException(HistorialJpa.class, h.getUsuarioId().valor());
            }
            var nuevo = new HistorialJpa();
            nuevo.usuarioId = h.getUsuarioId().valor();
            copiar(h, nuevo);
            em.persist(nuevo);
            em.flush();
            return nuevo.aDominio();
        }
        var j = gestionado.get();
        if (h.getVersion() != null && !Objects.equals(j.version, h.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(HistorialJpa.class, h.getUsuarioId().valor());
        }
        copiar(h, j);
        em.flush();
        return j.aDominio();
    }

    private static void copiar(HistorialCrediticio h, HistorialJpa j) {
        j.nivel = h.getNivel();
        j.creditosPagadosEnNivel = h.getCreditosPagadosEnNivel();
        j.maxDiasAtraso = h.getMaxDiasAtraso();
        j.bloqueadoInterno = h.isBloqueadoInterno();
    }
}

interface HistorialJpaRepository extends JpaRepository<HistorialJpa, Long> {}

/** evaluacion.historial_crediticio: una fila por cliente, creada con su primer crédito. */
@Entity
@Table(schema = "evaluacion", name = "historial_crediticio")
class HistorialJpa {

    @Id
    @Column(name = "usuario_id")
    Long usuarioId;

    @Column(nullable = false)
    Integer nivel;

    @Column(name = "creditos_pagados_en_nivel", nullable = false)
    Integer creditosPagadosEnNivel;

    @Column(name = "max_dias_atraso", nullable = false)
    Integer maxDiasAtraso;

    @Column(name = "bloqueado_interno", nullable = false)
    boolean bloqueadoInterno;

    @Version
    @Column(nullable = false)
    Long version;

    HistorialCrediticio aDominio() {
        return HistorialCrediticio.reconstituir(new UsuarioId(usuarioId), nivel, creditosPagadosEnNivel, maxDiasAtraso,
                bloqueadoInterno, version);
    }
}
