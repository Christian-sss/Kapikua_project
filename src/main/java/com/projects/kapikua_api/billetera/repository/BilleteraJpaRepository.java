package com.projects.kapikua_api.billetera.repository;

import com.projects.kapikua_api.billetera.entity.BilleteraEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface BilleteraJpaRepository extends JpaRepository<BilleteraEntity, Long> {
    Optional<BilleteraEntity> findByUsuarioId(Long usuarioId);

    Optional<BilleteraEntity> findByNumeroCelular(String numeroCelular);

    boolean existsByNumeroCelular(String numeroCelular);
}
