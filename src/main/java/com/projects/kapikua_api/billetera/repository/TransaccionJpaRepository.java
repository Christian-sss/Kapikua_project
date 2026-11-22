package com.projects.kapikua_api.billetera.repository;

import com.projects.kapikua_api.billetera.entity.TransaccionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface TransaccionJpaRepository extends JpaRepository<TransaccionEntity, Long> {
    Optional<TransaccionEntity> findByReferencia(String referencia);
}
