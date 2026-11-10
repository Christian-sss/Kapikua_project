package com.projects.kapikua_api.credito.application.port.out;

import com.projects.kapikua_api.credito.domain.model.Oferta;
import com.projects.kapikua_api.shared.UsuarioId;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface OfertaRepositoryPort {

    Optional<Oferta> porId(Long ofertaId);

    /** La oferta más reciente del cliente, en cualquier estado. */
    Optional<Oferta> ultimaDe(UsuarioId usuarioId);

    /** Ofertas con una invitación PENDIENTE para este aval. */
    List<Oferta> conInvitacionPendientePara(UsuarioId avalId);

    /** RN-21: el usuario ya respalda un crédito (aval ACEPTADO o ACTIVADO). */
    boolean tieneGarantiaActiva(UsuarioId avalId);

    /** RN-21, avales cruzados: {@code avalDe} ya es aval (en curso) de {@code deudor}. */
    boolean esAvalEnCursoDe(UsuarioId avalDe, UsuarioId deudor);

    /**
     * Para la tarea diaria: ofertas en curso cuya vigencia terminó antes de {@code hoy}, u ofertas con una invitación
     * pendiente enviada antes de {@code invitadasAntesDe} (RN-19, RN-21).
     */
    List<Long> idsConVencimientos(LocalDate hoy, OffsetDateTime invitadasAntesDe);

    /** Con bloqueo optimista: si otra transacción la cambió, lanza OptimisticLockingFailureException. */
    Oferta guardar(Oferta oferta);
}
