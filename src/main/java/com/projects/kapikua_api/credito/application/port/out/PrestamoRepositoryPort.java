package com.projects.kapikua_api.credito.application.port.out;

import com.projects.kapikua_api.credito.domain.model.Prestamo;
import com.projects.kapikua_api.shared.UsuarioId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PrestamoRepositoryPort {
    Optional<Prestamo> porId(Long prestamoId);

    Optional<Prestamo> activoDe(UsuarioId usuarioId);

    boolean tienePrestamoEnMora(UsuarioId usuarioId);

    Prestamo registrar(Prestamo prestamo);

    Prestamo guardar(Prestamo prestamo);

    List<Long> idsConCobroPendiente(LocalDate hoy);

    List<Long> idsConAvalActivadoDe(UsuarioId avalId);

    List<Long> idsQueRespalda(UsuarioId avalId);

    boolean ventaYaRetenida(Long transaccionVentaId);

    boolean pagadoPorAval(Long prestamoId);
}
