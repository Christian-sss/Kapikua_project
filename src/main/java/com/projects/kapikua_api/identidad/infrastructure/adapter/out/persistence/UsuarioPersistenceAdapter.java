package com.projects.kapikua_api.identidad.infrastructure.adapter.out.persistence;

import com.projects.kapikua_api.identidad.application.port.out.UsuarioRepositoryPort;

public class UsuarioPersistenceAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository usuarioJpaRepository;

    public UsuarioPersistenceAdapter(UsuarioJpaRepository usuarioJpaRepository) {
        this.usuarioJpaRepository = usuarioJpaRepository;
    }



}
