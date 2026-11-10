package com.projects.kapikua_api.evaluacion.domain.model;

import com.projects.kapikua_api.shared.UsuarioId;

import java.util.Objects;

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

    public static HistorialCrediticio reconstituir(UsuarioId usuarioId, int nivel, int creditosPagadosEnNivel,
                                                   int maxDiasAtraso, boolean bloqueadoInterno, Long version) {
        return new HistorialCrediticio(usuarioId, nivel, creditosPagadosEnNivel, maxDiasAtraso, bloqueadoInterno, version);
    }

    public boolean esNuevo() {
        return nivel == 1 && creditosPagadosEnNivel == 0 && maxDiasAtraso == 0;
    }

    public void registrarDesembolso() {
        maxDiasAtraso = 0;
    }

    public void registrarAtraso(int dias, ParametrosCredito p) {
        var anterior = maxDiasAtraso;
        maxDiasAtraso = Math.max(maxDiasAtraso, dias);
        if (anterior < p.diasAtrasoBajaNivel() && dias >= p.diasAtrasoBajaNivel()) {
            nivel = Math.max(1, nivel - 1);
            creditosPagadosEnNivel = 0;
        }
        if (dias >= p.diasAtrasoBloqueo()) {
            bloqueadoInterno = true;
        }
    }

    public void registrarCreditoPagado(boolean pagadoPorAval, ParametrosCredito p) {
        if (pagadoPorAval) {
            bloqueadoInterno = true;
            return;
        }
        if (maxDiasAtraso > p.diasAtrasoMaxParaSubir()) return;
        creditosPagadosEnNivel++;
        var condiciones = p.nivel(nivel);
        if (nivel < p.nivelMaximo() && creditosPagadosEnNivel >= condiciones.creditosParaSubir()) {
            nivel++;
            creditosPagadosEnNivel = 0;
        }
    }

    public UsuarioId getUsuarioId() { return usuarioId; }
    public int getNivel() { return nivel; }
    public int getCreditosPagadosEnNivel() { return creditosPagadosEnNivel; }
    public int getMaxDiasAtraso() { return maxDiasAtraso; }
    public boolean isBloqueadoInterno() { return bloqueadoInterno; }
    public Long getVersion() { return version; }
}
