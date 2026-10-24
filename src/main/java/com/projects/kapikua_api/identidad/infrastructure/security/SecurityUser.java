package com.projects.kapikua_api.identidad.infrastructure.security;

import com.projects.kapikua_api.identidad.domain.model.Usuario;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SecurityUser implements UserDetails {

    private final Usuario usuario;

    public SecurityUser(Usuario usuario) {
        this.usuario = usuario;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        List<GrantedAuthority> list = new ArrayList<>();

        list.add(new SimpleGrantedAuthority("ROLE_" + usuario.getRolUsuario().name()));

        return list;

    }

    @Override
    public @Nullable String getPassword() {
        return us;
    }

    @Override
    public String getUsername() {
        return "";
    }
}
