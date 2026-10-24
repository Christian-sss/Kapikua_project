package com.projects.kapikua_api.identidad.infrastructure.adapter.out.persistence;

import com.projects.kapikua_api.identidad.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioJpaRepository extends JpaRepository<Usuario,Long> {

}
