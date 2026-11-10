package com.projects.kapikua_api.shared.infrastructure.sse;

import com.projects.kapikua_api.shared.UsuarioAutenticado;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
class EventosController {
    private final SseNotificador notificador;

    EventosController(SseNotificador notificador) {
        this.notificador = notificador;
    }

    @GetMapping(path = "/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter conectar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return notificador.conectar(usuario.usuarioId());
    }
}
