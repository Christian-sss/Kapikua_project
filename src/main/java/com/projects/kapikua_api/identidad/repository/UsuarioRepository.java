package com.projects.kapikua_api.identidad.repository;

import com.projects.kapikua_api.identidad.entity.Usuario;
import com.projects.kapikua_api.shared.UsuarioId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByDni(String dni);

    boolean existsByCorreo(String correo);

    Optional<Usuario> findByCorreoOrDni(String correo, String dni);

    default Optional<Usuario> porId(UsuarioId id) {
        return findById(id.valor());
    }

    /** Busca por correo (sin distinguir mayúsculas) o por DNI. */
    default Optional<Usuario> porCorreoODni(String correoODni) {
        var valor = correoODni == null ? "" : correoODni.trim();
        return findByCorreoOrDni(valor.toLowerCase(), valor);
    }
}
