package com.projects.kapikua_api.identidad.infrastructure.adapter.out.persistence;

import com.projects.kapikua_api.identidad.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface UsuarioJpaRepository extends JpaRepository<Usuario, Long> {

    boolean existsByDni(String dni);

    boolean existsByCorreo(String correo);

    Optional<Usuario> findByCorreoOrDni(String correo, String dni);
}
