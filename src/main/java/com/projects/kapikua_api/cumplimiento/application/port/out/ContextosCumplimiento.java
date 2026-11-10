package com.projects.kapikua_api.cumplimiento.application.port.out;

import com.projects.kapikua_api.cumplimiento.domain.model.OperacionObservada;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.UsuarioId;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** Lo que Cumplimiento necesita de Billetera e Identidad. */
public interface ContextosCumplimiento {

    List<OperacionObservada> operaciones(Long billeteraId, OffsetDateTime desde, OffsetDateTime hasta);

    Optional<UsuarioId> titular(Long billeteraId);

    boolean billeteraBloqueada(Long billeteraId);

    /** Dentro de la transacción del llamador. Vacío si se bloqueó; si no, el motivo del fallo. */
    Optional<String> bloquear(Long billeteraId, String motivo);

    /** Si la operación es de la billetera del usuario (CU-15, 1). */
    boolean operacionDelUsuario(UsuarioId usuarioId, Long transaccionId);

    /** Nombre y DNI enmascarado (RN-30). */
    Optional<DatosCliente> cliente(UsuarioId usuarioId);

    record DatosCliente(String nombre, String dniEnmascarado) {}

    /** RN-01: tope por operación vigente (UIT × factor). */
    Dinero topePorOperacion(LocalDate fecha);
}
