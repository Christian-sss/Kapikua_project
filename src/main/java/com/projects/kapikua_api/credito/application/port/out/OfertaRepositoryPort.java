package com.projects.kapikua_api.credito.application.port.out;

import com.projects.kapikua_api.credito.domain.model.Oferta;
import com.projects.kapikua_api.shared.UsuarioId;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface OfertaRepositoryPort {
    Optional<Oferta> porId(Long ofertaId);

    Optional<Oferta> ultimaDe(UsuarioId usuarioId);

    List<Oferta> conInvitacionPendientePara(UsuarioId avalId);

    boolean tieneGarantiaActiva(UsuarioId avalId);

    boolean esAvalEnCursoDe(UsuarioId avalDe, UsuarioId deudor);

    List<Long> idsConVencimientos(LocalDate hoy, OffsetDateTime invitadasAntesDe);

    Oferta guardar(Oferta oferta);
}
