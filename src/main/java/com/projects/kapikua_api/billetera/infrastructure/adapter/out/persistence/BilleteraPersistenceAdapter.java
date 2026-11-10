package com.projects.kapikua_api.billetera.infrastructure.adapter.out.persistence;

import com.projects.kapikua_api.billetera.application.port.out.BilleteraRepositoryPort;
import com.projects.kapikua_api.billetera.domain.model.Billetera;
import com.projects.kapikua_api.billetera.domain.model.Celular;
import com.projects.kapikua_api.shared.UsuarioId;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

@Repository
class BilleteraPersistenceAdapter implements BilleteraRepositoryPort {

    private final BilleteraJpaRepository repositorio;
    private final EntityManager em;

    BilleteraPersistenceAdapter(BilleteraJpaRepository repositorio, EntityManager em) {
        this.repositorio = repositorio;
        this.em = em;
    }

    @Override
    public Optional<Billetera> porUsuario(UsuarioId usuarioId) {
        return repositorio.findByUsuarioId(usuarioId.valor()).map(BilleteraJpa::aDominio);
    }

    @Override
    public Optional<Billetera> porCelular(Celular celular) {
        return repositorio.findByNumeroCelular(celular.numero()).map(BilleteraJpa::aDominio);
    }

    @Override
    public Optional<Billetera> porId(Long billeteraId) {
        return repositorio.findById(billeteraId).map(BilleteraJpa::aDominio);
    }

    @Override
    public boolean existeCelular(Celular celular) {
        return repositorio.existsByNumeroCelular(celular.numero());
    }

    /**
     * Copia el estado del dominio sobre la entidad gestionada. Si su versión ya no es la que se leyó,
     * otra transacción la cambió: se lanza el conflicto para que el ejecutor reintente (RNF-08).
     * Además, Hibernate actualiza con "WHERE version = ?", lo que cubre el cambio concurrente hasta el commit.
     */
    @Override
    public Billetera guardar(Billetera billetera) {
        if (billetera.getId() == null) {
            var nueva = repositorio.saveAndFlush(BilleteraJpa.desde(billetera));
            return nueva.aDominio();
        }
        var gestionada = repositorio.findById(billetera.getId())
                .orElseThrow(() -> new IllegalStateException("Billetera inexistente: " + billetera.getId()));
        if (!Objects.equals(gestionada.version, billetera.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(BilleteraJpa.class, billetera.getId());
        }
        var cambios = BilleteraJpa.desde(billetera);
        gestionada.saldo = cambios.saldo;
        gestionada.saldoApartado = cambios.saldoApartado;
        gestionada.estado = cambios.estado;
        gestionada.motivoBloqueo = cambios.motivoBloqueo;
        em.flush();
        return gestionada.aDominio();
    }
}

interface BilleteraJpaRepository extends JpaRepository<BilleteraJpa, Long> {

    Optional<BilleteraJpa> findByUsuarioId(Long usuarioId);

    Optional<BilleteraJpa> findByNumeroCelular(String numeroCelular);

    boolean existsByNumeroCelular(String numeroCelular);
}
