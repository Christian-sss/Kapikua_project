package com.projects.kapikua_api.cumplimiento.client;

import com.projects.kapikua_api.cumplimiento.model.OperacionObservada;
import com.projects.kapikua_api.shared.UsuarioId;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** Lo que Cumplimiento necesita de Billetera e Identidad, traducido a su propio lenguaje. */
public interface ContextosCumplimiento {
    List<OperacionObservada> operaciones(Long billeteraId, OffsetDateTime desde, OffsetDateTime hasta);

    Optional<UsuarioId> titular(Long billeteraId);

    boolean billeteraBloqueada(Long billeteraId);

    Optional<String> bloquear(Long billeteraId, String motivo);

    boolean operacionDelUsuario(UsuarioId usuarioId, Long transaccionId);

    Optional<DatosCliente> cliente(UsuarioId usuarioId);

    record DatosCliente(String nombre, String dniEnmascarado) {}
}
