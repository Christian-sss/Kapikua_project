package com.projects.kapikua_api.billetera.infrastructure.adapter.in.evento;

import com.projects.kapikua_api.billetera.application.port.in.ConsultarBilleteraUseCase;
import com.projects.kapikua_api.billetera.domain.event.OperacionRegistrada;
import com.projects.kapikua_api.shared.puerto.Notificador;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Objects;
import java.util.stream.Stream;

@Component
class AvisosBilletera {
    private final ConsultarBilleteraUseCase billeteras;
    private final Notificador notificador;

    AvisosBilletera(ConsultarBilleteraUseCase billeteras, Notificador notificador) {
        this.billeteras = billeteras;
        this.notificador = notificador;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void alRegistrarse(OperacionRegistrada evento) {
        Stream.of(evento.billeteraOrigenId(), evento.billeteraDestinoId())
                .filter(Objects::nonNull)
                .distinct()
                .map(billeteras::titularDe)
                .flatMap(java.util.Optional::stream)
                .forEach(usuario -> notificador.notificar(usuario, Notificador.Aviso.BILLETERA));
    }
}
