package com.projects.kapikua_api.identidad.infrastructure.security;

import com.projects.kapikua_api.identidad.application.port.out.UsuarioRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Sesión sin estado en el servidor: el JWT viaja en una cookie HttpOnly (SAD 2.6).
 * La protección CSRF de los formularios sigue activa; su token se guarda en otra cookie,
 * así tampoco se crea una HttpSession.
 */
@Configuration
public class SecurityConfig {

    /** RNF-01: BCrypt con factor de costo 10 o más. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain configure(HttpSecurity http,
                                         JwtServicio jwt,
                                         CookieSesion cookie,
                                         UsuarioRepositoryPort usuarios) throws Exception {
        return http
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(RequestCacheConfigurer::disable)
                .csrf(c -> c.csrfTokenRepository(new CookieCsrfTokenRepository()))
                .authorizeHttpRequests(c -> c
                        .requestMatchers("/registro", "/login", "/css/**", "/js/**", "/error").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMINISTRADOR")   // 403 para otro rol (RNF-05)
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtCookieFilter(jwt, cookie, usuarios), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e.authenticationEntryPoint((request, response, ex) ->
                        response.sendRedirect(request.getContextPath()
                                + (request.getAttribute(JwtCookieFilter.SESION_VENCIDA) != null
                                ? "/login?vencida" : "/login"))))
                .logout(l -> l
                        .logoutUrl("/logout")
                        .addLogoutHandler((request, response, auth) -> cookie.borrar(response))
                        .logoutSuccessUrl("/login?salir"))
                .build();
    }
}
