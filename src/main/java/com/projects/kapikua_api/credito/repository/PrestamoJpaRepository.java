package com.projects.kapikua_api.credito.repository;

import com.projects.kapikua_api.credito.entity.PrestamoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

interface PrestamoJpaRepository extends JpaRepository<PrestamoEntity, Long> {
    Optional<PrestamoEntity> findFirstByUsuarioIdAndEstadoIn(Long usuarioId, Collection<String> estados);

    boolean existsByUsuarioIdAndEstado(Long usuarioId, String estado);
}
