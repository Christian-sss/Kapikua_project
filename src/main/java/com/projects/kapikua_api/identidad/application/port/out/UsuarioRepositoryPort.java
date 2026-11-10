package com.projects.kapikua_api.identidad.application.port.out;

import com.projects.kapikua_api.identidad.domain.model.Usuario;
import com.projects.kapikua_api.shared.UsuarioId;

import java.util.Optional;

public interface UsuarioRepositoryPort {
    Optional<Usuario> porId(UsuarioId id);

    boolean existeDni(String dni);

    boolean existeCorreo(String correo);

    Optional<Usuario> porCorreoODni(String correoODni);

    Usuario guardar(Usuario usuario);
}
