package com.projects.kapikua_api.evaluacion.domain.model;

import com.projects.kapikua_api.shared.UsuarioId;
import lombok.Getter;

import java.util.Objects;

@Getter
public final class HistorialCrediticio {
    private final UsuarioId usuarioId;
    private int nivel;
    private int creditosPagadosEnNivel;
    private int maxDiasAtraso;
    private boolean bloqueadoInterno;
    private final Long version;

    private HistorialCrediticio(UsuarioId usuarioId, int nivel, int creditosPagadosEnNivel, int maxDiasAtraso,
                                boolean bloqueadoInterno, Long version) {
        this.usuarioId = Objects.requireNonNull(usuarioId);
        if (nivel < 1) throw new IllegalArgumentException("Nivel inválido: " + nivel);
        this.nivel = nivel;
        this.creditosPagadosEnNivel = creditosPagadosEnNivel;
        this.maxDiasAtraso = maxDiasAtraso;
        this.bloqueadoInterno = bloqueadoInterno;
        this.version = version;
    }

    public static HistorialCrediticio nuevo(UsuarioId usuarioId) {
        return new HistorialCrediticio(usuarioId, 1, 0, 0, false, null);
    }

    public static HistorialCrediticio reconstituir(
            UsuarioId usuarioId,
            int nivel,
            int creditosPagadosEnNivel,
            int maxDiasAtraso,
            boolean bloqueadoInterno,
            Long version
    ) {
        return new HistorialCrediticio(usuarioId, nivel, creditosPagadosEnNivel, maxDiasAtraso, bloqueadoInterno, version);
    }



    public boolean esNuevo() {
        return nivel == 1 && creditosPagadosEnNivel == 0 && maxDiasAtraso == 0;
    }

    public void registrarDesembolso() {
        maxDiasAtraso = 0;
    }

    public void registrarAtraso(int dias, ReglasNivel reglas) {
        var anterior = maxDiasAtraso;
        maxDiasAtraso = Math.max(maxDiasAtraso, dias);
        if (anterior < reglas.diasAtrasoBajaNivel() && dias >= reglas.diasAtrasoBajaNivel()) {
            nivel = Math.max(1, nivel - 1);
            creditosPagadosEnNivel = 0;
        }
        if (dias >= reglas.diasAtrasoBloqueo()) {
            bloqueadoInterno = true;
        }
    }

    public void registrarCreditoPagado(boolean pagadoPorAval, ReglasNivel reglas) {
        if (pagadoPorAval) {
            bloqueadoInterno = true;
            return;
        }
        if (maxDiasAtraso > reglas.diasAtrasoMaxParaSubir()) return;
        creditosPagadosEnNivel++;
        var condiciones = reglas.nivel(nivel);
        if (nivel < reglas.nivelMaximo() && creditosPagadosEnNivel >= condiciones.creditosParaSubir()) {
            nivel++;
            creditosPagadosEnNivel = 0;
        }
    }
}
