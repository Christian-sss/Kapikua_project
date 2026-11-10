package com.projects.kapikua_api.billetera.infrastructure.adapter.in.evento;

import com.projects.kapikua_api.billetera.application.port.in.AbrirBilleteraUseCase;
import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.evento.UsuarioRegistrado;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class OyenteUsuarioRegistrado {
    private final AbrirBilleteraUseCase abrirBilletera;

    OyenteUsuarioRegistrado(AbrirBilleteraUseCase abrirBilletera) {
        this.abrirBilletera = abrirBilletera;
    }

    @EventListener
    void alRegistrarse(UsuarioRegistrado evento) {
        var resultado = abrirBilletera.abrir(evento.usuarioId(), evento.celular(), evento.nombreNegocio());
        if (resultado instanceof Result.Err<Long, ErrorBilletera> err) {
            throw new IllegalStateException("No se pudo abrir la billetera: " + err.error().codigo());
        }
    }
}
