package com.projects.kapikua_api.identidad.infrastructure.adapter.out.persistence;

import com.projects.kapikua_api.identidad.application.port.out.UsuarioRepositoryPort;
import com.projects.kapikua_api.identidad.domain.model.Usuario;
import com.projects.kapikua_api.shared.UsuarioId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class UsuarioPersistenceAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository repositorio;

    UsuarioPersistenceAdapter(UsuarioJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Optional<Usuario> porId(UsuarioId id) {
        return repositorio.findById(id.valor());
    }

    @Override
    public boolean existeDni(String dni) {
        return repositorio.existsByDni(dni);
    }

    @Override
    public boolean existeCorreo(String correo) {
        return repositorio.existsByCorreo(correo);
    }

    @Override
    public Optional<Usuario> porCorreoODni(String correoODni) {
        var valor = correoODni == null ? "" : correoODni.trim();
        return repositorio.findByCorreoOrDni(valor.toLowerCase(), valor);
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        return repositorio.save(usuario);
    }
}
