package com.projects.kapikua_api.billetera.repository;

import com.projects.kapikua_api.billetera.entity.BilleteraEntity;
import com.projects.kapikua_api.billetera.mapper.BilleteraMapper;
import com.projects.kapikua_api.billetera.model.Billetera;
import com.projects.kapikua_api.billetera.model.Celular;
import com.projects.kapikua_api.shared.UsuarioId;
import jakarta.persistence.EntityManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

/** Lee y guarda billeteras. Convierte entre la fila y el modelo con BilleteraMapper. */
@Repository
public class BilleteraRepository {
    private final BilleteraJpaRepository repositorio;
    private final EntityManager em;

    public BilleteraRepository(BilleteraJpaRepository repositorio, EntityManager em) {
        this.repositorio = repositorio;
        this.em = em;
    }

    public Optional<Billetera> porUsuario(UsuarioId usuarioId) {
        return repositorio.findByUsuarioId(usuarioId.valor()).map(BilleteraMapper::aModelo);
    }

    public Optional<Billetera> porCelular(Celular celular) {
        return repositorio.findByNumeroCelular(celular.numero()).map(BilleteraMapper::aModelo);
    }

    public Optional<Billetera> porId(Long billeteraId) {
        return repositorio.findById(billeteraId).map(BilleteraMapper::aModelo);
    }

    public boolean existeCelular(Celular celular) {
        return repositorio.existsByNumeroCelular(celular.numero());
    }

    /**
     * Inserta la billetera nueva o copia sus cambios a la fila, y hace flush en el momento: así un conflicto de
     * versión salta aquí y se respeta el orden en que el servicio guarda dos billeteras.
     */
    public Billetera guardar(Billetera billetera) {
        if (billetera.getId() == null) {
            return BilleteraMapper.aModelo(repositorio.saveAndFlush(BilleteraMapper.aEntidadNueva(billetera)));
        }
        var fila = repositorio.findById(billetera.getId())
                .orElseThrow(() -> new IllegalStateException("Billetera inexistente: " + billetera.getId()));
        if (!Objects.equals(fila.getVersion(), billetera.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(BilleteraEntity.class, billetera.getId());
        }
        BilleteraMapper.copiarCambios(billetera, fila);
        em.flush();
        return BilleteraMapper.aModelo(fila);
    }
}
