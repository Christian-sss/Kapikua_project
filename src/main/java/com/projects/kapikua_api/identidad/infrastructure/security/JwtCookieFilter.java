package com.projects.kapikua_api.identidad.infrastructure.security;

import com.projects.kapikua_api.identidad.application.port.out.UsuarioRepositoryPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

class JwtCookieFilter extends OncePerRequestFilter {
    static final String SESION_VENCIDA = "kapikua.sesionVencida";

    private final JwtServicio jwt;
    private final CookieSesion cookie;
    private final UsuarioRepositoryPort usuarios;
    private final SecurityContextRepository repositorioContexto;

    JwtCookieFilter(JwtServicio jwt, CookieSesion cookie, UsuarioRepositoryPort usuarios,
                    SecurityContextRepository repositorioContexto) {
        this.repositorioContexto = repositorioContexto;
        this.jwt = jwt;
        this.cookie = cookie;
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var token = cookie.leer(request);
        if (token.isPresent()) {
            var usuario = jwt.leer(token.get()).flatMap(usuarios::porId).filter(u -> u.isActivo());
            if (usuario.isPresent()) {
                var principal = new SecurityUser(usuario.get());
                var auth = UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
                var contexto = SecurityContextHolder.createEmptyContext();
                contexto.setAuthentication(auth);
                SecurityContextHolder.setContext(contexto);
                repositorioContexto.saveContext(contexto, request, response);
                if (!esPeticionAutomatica(request)) {
                    cookie.escribir(request, response, jwt.emitir(principal.usuarioId(), usuario.get().getRol()), jwt.duracion());
                }
            } else {
                request.setAttribute(SESION_VENCIDA, Boolean.TRUE);
                cookie.borrar(request, response);
            }
        }
        chain.doFilter(request, response);
    }

    static boolean esPeticionAutomatica(HttpServletRequest request) {
        var ruta = request.getServletPath();
        return ruta.equals("/eventos") || ruta.contains("/fragmentos/");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var ruta = request.getServletPath();
        return ruta.startsWith("/css/") || ruta.startsWith("/js/");
    }
}
