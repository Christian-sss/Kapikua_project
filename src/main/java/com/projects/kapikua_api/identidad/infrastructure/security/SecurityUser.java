package com.projects.kapikua_api.identidad.infrastructure.security;

import com.projects.kapikua_api.identidad.domain.model.Usuario;
import com.projects.kapikua_api.shared.UsuarioAutenticado;
import com.projects.kapikua_api.shared.UsuarioId;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

/**
 * Usuario autenticado de la petición. Los controladores de otros contextos lo reciben con
 * {@code @AuthenticationPrincipal SecurityUser} y solo deberían usar {@link #usuarioId()}.
 */
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
