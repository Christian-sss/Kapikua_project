package com.projects.kapikua_api.billetera.domain.model;

import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

import java.time.OffsetDateTime;
import java.util.Objects;

public final class Billetera {
    private final Long id;
    private final UsuarioId usuarioId;
    private final Celular celular;
    private final String nombreNegocio;
    private Dinero saldo;
    private Dinero saldoApartado;
    private EstadoBilletera estado;
    private String motivoBloqueo;
    private final OffsetDateTime fechaApertura;
    private final Long version;

    private Billetera(Long id, UsuarioId usuarioId, Celular celular, String nombreNegocio, Dinero saldo,
                      Dinero saldoApartado, EstadoBilletera estado, String motivoBloqueo,
                      OffsetDateTime fechaApertura, Long version) {
        this.id = id;
        this.usuarioId = Objects.requireNonNull(usuarioId);
        this.celular = Objects.requireNonNull(celular);
        this.nombreNegocio = nombreNegocio == null || nombreNegocio.isBlank() ? null : nombreNegocio.trim();
        this.saldo = Objects.requireNonNull(saldo);
        this.saldoApartado = Objects.requireNonNull(saldoApartado);
        this.estado = Objects.requireNonNull(estado);
        this.motivoBloqueo = motivoBloqueo;
        this.fechaApertura = Objects.requireNonNull(fechaApertura);
        this.version = version;
    }

    public static Billetera abrir(UsuarioId usuarioId, Celular celular, String nombreNegocio, OffsetDateTime ahora) {
        return new Billetera(null, usuarioId, celular, nombreNegocio, Dinero.CERO, Dinero.CERO,
                EstadoBilletera.ACTIVA, null, ahora, null);
    }

    public static Billetera reconstituir(Long id, UsuarioId usuarioId, Celular celular, String nombreNegocio,
                                         Dinero saldo, Dinero saldoApartado, EstadoBilletera estado,
                                         String motivoBloqueo, OffsetDateTime fechaApertura, Long version) {
        return new Billetera(id, usuarioId, celular, nombreNegocio, saldo, saldoApartado, estado,
                motivoBloqueo, fechaApertura, version);
    }

    public Dinero saldoDisponible() {
        return saldo.restar(saldoApartado);
    }

    public boolean estaActiva() {
        return estado == EstadoBilletera.ACTIVA;
    }

    public boolean esNegocio() {
        return nombreNegocio != null;
    }

    public Dinero puedeRecibir(Dinero saldoMaximo) {
        return saldo.faltaPara(saldoMaximo);
    }

    public Result<Void, ErrorBilletera> debitar(Dinero monto) {
        if (!estaActiva()) return Result.err(new ErrorBilletera.BilleteraBloqueada());
        if (monto.esCero()) return Result.err(new ErrorBilletera.MontoInvalido());
        if (saldoDisponible().menorQue(monto)) return Result.err(new ErrorBilletera.SaldoInsuficiente());
        saldo = saldo.restar(monto);
        return Result.ok();
    }

    public Result<Void, ErrorBilletera> acreditar(Dinero monto, Dinero saldoMaximo, boolean esDestinoAjeno) {
        if (!estaActiva()) {
            return Result.err(esDestinoAjeno ? new ErrorBilletera.DestinatarioNoEncontrado()
                                             : new ErrorBilletera.BilleteraBloqueada());
        }
        if (monto.esCero()) return Result.err(new ErrorBilletera.MontoInvalido());
        if (saldo.sumar(monto).mayorQue(saldoMaximo)) {
            return Result.err(esDestinoAjeno ? new ErrorBilletera.DestinoNoPuedeRecibir()
                                             : new ErrorBilletera.TopeSaldoPropio(puedeRecibir(saldoMaximo)));
        }
        saldo = saldo.sumar(monto);
        return Result.ok();
    }

    public Dinero apartar(Dinero monto) {
        var posible = monto.menorQue(saldoDisponible()) ? monto : saldoDisponible();
        saldoApartado = saldoApartado.sumar(posible);
        return posible;
    }

    public void liberarApartado(Dinero monto) {
        saldoApartado = saldoApartado.mayorQue(monto) ? saldoApartado.restar(monto) : Dinero.CERO;
    }

    public void consumirApartado(Dinero monto) {
        if (monto.mayorQue(saldoApartado)) throw new IllegalArgumentException("No hay tanto apartado");
        saldoApartado = saldoApartado.restar(monto);
        saldo = saldo.restar(monto);
    }

    public void bloquear(String motivo) {
        if (motivo == null || motivo.isBlank()) throw new IllegalArgumentException("El bloqueo requiere un motivo");
        if (!estaActiva()) return;
        estado = EstadoBilletera.BLOQUEADA;
        var texto = motivo.strip();
        motivoBloqueo = texto.length() > 150 ? texto.substring(0, 150) : texto;
    }

    public Long getId() { return id; }
    public UsuarioId getUsuarioId() { return usuarioId; }
    public Celular getCelular() { return celular; }
    public String getNombreNegocio() { return nombreNegocio; }
    public Dinero getSaldo() { return saldo; }
    public Dinero getSaldoApartado() { return saldoApartado; }
    public EstadoBilletera getEstado() { return estado; }
    public String getMotivoBloqueo() { return motivoBloqueo; }
    public OffsetDateTime getFechaApertura() { return fechaApertura; }
    public Long getVersion() { return version; }
}
