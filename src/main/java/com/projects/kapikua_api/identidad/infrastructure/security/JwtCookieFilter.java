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

/**
 * Lee el JWT de la cookie y autentica la petición. En cada petición válida emite un token nuevo,
 * de modo que la sesión vence solo tras 15 minutos sin actividad (RNF-03).
 * El usuario se vuelve a leer de la base para que un usuario desactivado pierda el acceso de inmediato
 * y su rol esté siempre al día.
 */
class JwtCookieFilter extends OncePerRequestFilter {

    /** Atributo que indica al punto de entrada que la sesión venció (CU-02, 3a). */
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
                // Se guarda en la petición para el despacho asíncrono de SSE, donde este filtro no vuelve a correr.
                repositorioContexto.saveContext(contexto, request, response);
                if (!esPeticionAutomatica(request)) {
                    cookie.escribir(response, jwt.emitir(principal.usuarioId(), usuario.get().getRol()), jwt.duracion());
                }
            } else {
                request.setAttribute(SESION_VENCIDA, Boolean.TRUE);
                cookie.borrar(response);
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * Peticiones que hace la pantalla sola, sin que el usuario actúe: la conexión SSE y los fragmentos
     * que se recargan con cada aviso. No renuevan la sesión; si lo hicieran, una pestaña abierta la
     * mantendría viva para siempre y no vencería por inactividad (RNF-03).
     */
    static boolean esPeticionAutomatica(HttpServletRequest request) {
        var ruta = request.getServletPath();
        return ruta.equals("/eventos") || ruta.contains("/fragmentos/");
    }

    /** Los archivos estáticos no necesitan sesión ni renovar la cookie. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var ruta = request.getServletPath();
        return ruta.startsWith("/css/") || ruta.startsWith("/js/");
    }
}
