package com.projects.kapikua_api.billetera.evento;

import com.projects.kapikua_api.billetera.service.BilleteraService;
import com.projects.kapikua_api.billetera.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.evento.UsuarioRegistrado;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class OyenteUsuarioRegistrado {
    private final BilleteraService abrirBilletera;

    OyenteUsuarioRegistrado(BilleteraService abrirBilletera) {
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
