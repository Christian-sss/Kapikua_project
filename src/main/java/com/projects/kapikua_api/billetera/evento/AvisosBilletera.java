package com.projects.kapikua_api.billetera.evento;

import com.projects.kapikua_api.billetera.service.ConsultaBilleteraService;
import com.projects.kapikua_api.shared.evento.TransaccionRegistrada;
import com.projects.kapikua_api.shared.puerto.Notificador;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/** Avisa en pantalla a los titulares de las dos billeteras cuando una operación ya quedó guardada. */
@Component
class AvisosBilletera {
    private final ConsultaBilleteraService billeteras;
    private final Notificador notificador;

    AvisosBilletera(ConsultaBilleteraService billeteras, Notificador notificador) {
        this.billeteras = billeteras;
        this.notificador = notificador;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void alRegistrarse(TransaccionRegistrada evento) {
        Stream.of(evento.billeteraOrigenId(), evento.billeteraDestinoId())
                .filter(Objects::nonNull)
                .distinct()
                .map(billeteras::titularDe)
                .flatMap(Optional::stream)
                .forEach(usuario -> notificador.notificar(usuario, Notificador.Aviso.BILLETERA));
    }
}
