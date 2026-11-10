package com.projects.kapikua_api.evaluacion.domain.model;

import com.projects.kapikua_api.shared.UsuarioId;

import java.util.Objects;

/**
 * Raíz de agregado: el historial del cliente en Kapikua y su nivel en la escalera (RN-14, RN-26, RN-32).
 * {@code maxDiasAtraso} es el mayor atraso del crédito en curso o, si no hay, del último pagado:
 * se reinicia con cada desembolso y lo usa el score.
 */
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

    /** Todo cliente empieza en el nivel 1, sin créditos. */
    public static HistorialCrediticio nuevo(UsuarioId usuarioId) {
        return new HistorialCrediticio(usuarioId, 1, 0, 0, false, null);
    }

    public static HistorialCrediticio reconstituir(UsuarioId usuarioId, int nivel, int creditosPagadosEnNivel,
                                                   int maxDiasAtraso, boolean bloqueadoInterno, Long version) {
        return new HistorialCrediticio(usuarioId, nivel, creditosPagadosEnNivel, maxDiasAtraso, bloqueadoInterno, version);
    }

    /**
     * Sin créditos previos (factor historial del score = 50). Quien tuvo un crédito o subió de nivel,
     * o quien bajó al nivel 1 por un atraso (y quedó con días de atraso), ya no es nuevo.
     */
    public boolean esNuevo() {
        return nivel == 1 && creditosPagadosEnNivel == 0 && maxDiasAtraso == 0;
    }

    /** Al desembolsar un crédito nuevo, el atraso se vuelve a contar desde cero. */
    public void registrarDesembolso() {
        maxDiasAtraso = 0;
    }

    /**
     * RN-26: al llegar a {@code diasBajaNivel} (9) baja un nivel, una sola vez por crédito;
     * desde {@code diasBloqueo} (31) queda bloqueado. Se llama cada día que el crédito sigue atrasado.
     */
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

    /**
     * RN-26 y RN-32: si lo pagó el aval, el cliente queda bloqueado. Si no tuvo cuotas con más de
     * {@code diasAtrasoMaxParaSubir} (8) días de atraso, cuenta para subir de nivel.
     */
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
