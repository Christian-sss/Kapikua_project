package com.projects.kapikua_api.shared.infrastructure.sse;

import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.puerto.Notificador;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Adaptador del puerto Notificador con Server-Sent Events (SAD 2.4).
 * Cada pantalla abierta mantiene una conexión; un usuario puede tener varias (pestañas o dispositivos).
 * Las conexiones viven en memoria: con más de una instancia de la aplicación haría falta un canal compartido.
 */
@Component
public class SseNotificador implements Notificador {

    private static final Logger log = LoggerFactory.getLogger(SseNotificador.class);

    private final Map<UsuarioId, List<SseEmitter>> conexiones = new ConcurrentHashMap<>();
    private final Duration duracionConexion;

    public SseNotificador(@Value("${kapikua.sse.minutos-conexion:5}") long minutos) {
        this.duracionConexion = Duration.ofMinutes(minutos);
    }

    /**
     * Abre una conexión. Vence a los pocos minutos: el navegador se reconecta solo y, al reconectarse,
     * se vuelve a comprobar la sesión. Así una pantalla olvidada no recibe avisos con la sesión vencida.
     */
    public SseEmitter conectar(UsuarioId usuarioId) {
        var emisor = new SseEmitter(duracionConexion.toMillis());
        var lista = conexiones.computeIfAbsent(usuarioId, k -> new CopyOnWriteArrayList<>());
        lista.add(emisor);
        Runnable quitar = () -> quitar(usuarioId, emisor);
        emisor.onCompletion(quitar);
        emisor.onTimeout(quitar);
        emisor.onError(e -> quitar.run());
        enviar(usuarioId, emisor, SseEmitter.event().comment("conectado"));
        return emisor;
    }

    @Override
    public void notificar(UsuarioId usuarioId, Aviso aviso) {
        var lista = conexiones.get(usuarioId);
        if (lista == null) return;
        for (var emisor : lista) {
            enviar(usuarioId, emisor, SseEmitter.event().name(aviso.name().toLowerCase()).data("1"));
        }
    }

    /** Latido cada 25 s para que el proxy de Railway no cierre las conexiones inactivas (SAD 2.4). */
    @Scheduled(fixedRateString = "${kapikua.sse.latido-ms:25000}")
    void latido() {
        conexiones.forEach((usuario, lista) ->
                lista.forEach(emisor -> enviar(usuario, emisor, SseEmitter.event().comment("latido"))));
    }

    /**
     * Al apagar la aplicación se cierran las conexiones ANTES del cierre ordenado del servidor
     * (ContextClosedEvent llega primero). Si no, el servidor esperaría a que terminen conexiones de varios
     * minutos. Los navegadores se reconectan solos a la nueva instancia.
     */
    @EventListener(ContextClosedEvent.class)
    void alApagar() {
        conexiones.values().forEach(lista -> lista.forEach(SseEmitter::complete));
        conexiones.clear();
    }

    int conexionesAbiertas(UsuarioId usuarioId) {
        var lista = conexiones.get(usuarioId);
        return lista == null ? 0 : lista.size();
    }

    private void enviar(UsuarioId usuarioId, SseEmitter emisor, SseEmitter.SseEventBuilder evento) {
        try {
            emisor.send(evento);
        } catch (IOException | IllegalStateException e) {
            // La pantalla se cerró: se descarta la conexión. No es un error del sistema.
            log.debug("Conexión SSE cerrada para el usuario {}", usuarioId.valor());
            quitar(usuarioId, emisor);
            emisor.completeWithError(e);
        }
    }

    private void quitar(UsuarioId usuarioId, SseEmitter emisor) {
        conexiones.computeIfPresent(usuarioId, (k, lista) -> {
            lista.remove(emisor);
            return lista.isEmpty() ? null : lista;
        });
    }
}
