package com.projects.kapikua_api.shared.infrastructure.sse;

import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.puerto.Notificador;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SseNotificadorTest {

    private static final UsuarioId ANA = new UsuarioId(1L);
    private static final UsuarioId LUIS = new UsuarioId(2L);

    @Test
    void unUsuarioPuedeTenerVariasPantallasAbiertas() {
        var notificador = new SseNotificador(5);
        notificador.conectar(ANA);
        notificador.conectar(ANA);
        notificador.conectar(LUIS);
        assertThat(notificador.conexionesAbiertas(ANA)).isEqualTo(2);
        assertThat(notificador.conexionesAbiertas(LUIS)).isEqualTo(1);
    }

    @Test
    void avisarAQuienNoTienePantallasAbiertasNoFalla() {
        var notificador = new SseNotificador(5);
        notificador.notificar(ANA, Notificador.Aviso.BILLETERA);
        assertThat(notificador.conexionesAbiertas(ANA)).isZero();
    }

    @Test
    void alApagarSeCierranTodasLasConexiones() {
        var notificador = new SseNotificador(5);
        notificador.conectar(ANA);
        notificador.conectar(LUIS);
        notificador.alApagar();
        assertThat(notificador.conexionesAbiertas(ANA)).isZero();
        assertThat(notificador.conexionesAbiertas(LUIS)).isZero();
    }
}
