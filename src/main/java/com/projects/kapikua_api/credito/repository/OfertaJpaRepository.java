package com.projects.kapikua_api.credito.repository;

import com.projects.kapikua_api.credito.entity.OfertaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface OfertaJpaRepository extends JpaRepository<OfertaEntity, Long> {
    Optional<OfertaEntity> findFirstByUsuarioIdOrderByIdDesc(Long usuarioId);
}
