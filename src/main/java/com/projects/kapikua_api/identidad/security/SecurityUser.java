package com.projects.kapikua_api.identidad.security;

import com.projects.kapikua_api.identidad.entity.Usuario;
import com.projects.kapikua_api.shared.UsuarioAutenticado;
import com.projects.kapikua_api.shared.UsuarioId;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

public class SecurityUser implements UsuarioAutenticado {
    private final UsuarioId usuarioId;
    private final String nombres;
    private final String rol;

    public SecurityUser(Usuario usuario) {
        this.usuarioId = usuario.usuarioId();
        this.nombres = usuario.getNombres();
        this.rol = usuario.getRol().name();
    }

    @Override
    public UsuarioId usuarioId() {
        return usuarioId;
    }

    public String nombres() {
        return nombres;
    }

    public String rol() {
        return rol;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @Override
    public String toString() {
        return "SecurityUser[usuarioId=" + usuarioId + ", rol=" + rol + "]";
    }
}
