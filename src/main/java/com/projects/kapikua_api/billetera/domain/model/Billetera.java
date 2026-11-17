package com.projects.kapikua_api.billetera.domain.model;

import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.billetera.domain.event.EstadoCambiado;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

import java.time.OffsetDateTime;
import java.util.Objects;

public final class Billetera {

    private static final int MAXIMO_CARACTERES_MOTIVO = 150;

    private final Long id;
    private final UsuarioId usuarioId;
    private final Celular celular;
    private final String nombreNegocio;
    private Dinero saldo;
    private Dinero saldoApartado;
    private EstadoBilletera estado;
    private String motivoEstado;
    private final OffsetDateTime fechaApertura;
    private final Long version;

    private Billetera(Long id, UsuarioId usuarioId, Celular celular, String nombreNegocio, Dinero saldo,
                      Dinero saldoApartado, EstadoBilletera estado, String motivoEstado,
                      OffsetDateTime fechaApertura, Long version) {
        this.id = id;
        this.usuarioId = Objects.requireNonNull(usuarioId);
        this.celular = Objects.requireNonNull(celular);
        this.nombreNegocio = nombreNegocio == null || nombreNegocio.isBlank() ? null : nombreNegocio.trim();
        this.saldo = Objects.requireNonNull(saldo);
        this.saldoApartado = Objects.requireNonNull(saldoApartado);
        this.estado = Objects.requireNonNull(estado);
        this.motivoEstado = motivoEstado;
        this.fechaApertura = Objects.requireNonNull(fechaApertura);
        this.version = version;
    }

    public static Billetera abrir(UsuarioId usuarioId, Celular celular, String nombreNegocio, OffsetDateTime ahora) {
        return new Billetera(null, usuarioId, celular, nombreNegocio, Dinero.CERO, Dinero.CERO,
                EstadoBilletera.ACTIVA, null, ahora, null);
    }

    public static Billetera reconstituir(Long id, UsuarioId usuarioId, Celular celular, String nombreNegocio,
                                         Dinero saldo, Dinero saldoApartado, String estado,
                                         String motivoEstado, OffsetDateTime fechaApertura, Long version) {
        return new Billetera(id, usuarioId, celular, nombreNegocio, saldo, saldoApartado, EstadoBilletera.desdeNombre(estado),
                motivoEstado, fechaApertura, version);
    }

    public Dinero saldoDisponible() {
        return saldo.restar(saldoApartado);
    }

    public boolean estaActiva() {
        return estado.estaActiva();
    }

    public boolean estaBloqueada() {
        return estado.estaBloqueada();
    }

    public boolean esNegocio() {
        return nombreNegocio != null;
    }

    public Dinero puedeRecibir(Dinero saldoMaximo) {
        return saldo.faltaPara(saldoMaximo);
    }

    public Result<Void, ErrorBilletera> debitar(Dinero monto) {
        return validarSalida().flatMap(ignorado -> {
            if (monto.esCero()) return Result.err(new ErrorBilletera.MontoInvalido());
            if (saldoDisponible().menorQue(monto)) return Result.err(new ErrorBilletera.SaldoInsuficiente());
            saldo = saldo.restar(monto);
            return Result.ok();
        });
    }

    public Result<Void, ErrorBilletera> acreditar(Dinero monto, Dinero saldoMaximo, boolean esDestinoAjeno) {
        return validarAcreditacion(monto, saldoMaximo, esDestinoAjeno).map(ignorado -> {
            saldo = saldo.sumar(monto);
            return null;
        });
    }

    public Result<Void, ErrorBilletera> validarSalida() {
        return estado.validarSalida();
    }

    public Result<Void, ErrorBilletera> validarRecepcion(boolean esDestinoAjeno) {
        return estado.validarRecepcion(esDestinoAjeno);
    }

    public Result<Void, ErrorBilletera> validarAcreditacion(Dinero monto, Dinero saldoMaximo, boolean esDestinoAjeno) {
        return validarRecepcion(esDestinoAjeno).flatMap(ignorado -> {
            if (monto.esCero()) return Result.err(new ErrorBilletera.MontoInvalido());
            if (saldo.sumar(monto).mayorQue(saldoMaximo)) {
                return Result.err(esDestinoAjeno ? new ErrorBilletera.DestinoNoPuedeRecibir()
                        : new ErrorBilletera.TopeSaldoPropio(puedeRecibir(saldoMaximo)));
            }
            return Result.ok();
        });
    }

    public Result<Dinero, ErrorBilletera> apartar(Dinero monto) {
        return validarSalida().map(ignorado -> {
            var disponible = saldoDisponible();
            var posible = monto.menorQue(disponible) ? monto : disponible;
            saldoApartado = saldoApartado.sumar(posible);
            return posible;
        });
    }

    public Result<Dinero, ErrorBilletera> liberarApartado(Dinero monto) {
        return validarSalida().map(ignorado -> {
            var liberado = monto.menorQue(saldoApartado) ? monto : saldoApartado;
            saldoApartado = saldoApartado.restar(liberado);
            return liberado;
        });
    }

    public Result<Dinero, ErrorBilletera> consumirApartado(Dinero monto) {
        return validarSalida().flatMap(ignorado -> {
            if (monto.mayorQue(saldoApartado)) return Result.err(new ErrorBilletera.SaldoApartadoInsuficiente());
            saldoApartado = saldoApartado.restar(monto);
            saldo = saldo.restar(monto);
            return Result.ok(monto);
        });
    }

    public Result<EstadoCambiado, ErrorBilletera> bloquear(String motivo, OffsetDateTime fecha) {
        return normalizarMotivo(motivo)
                .flatMap(valor -> estado.bloquear().map(nuevo -> cambiarEstado(nuevo, valor, fecha)));
    }

    public Result<EstadoCambiado, ErrorBilletera> retener(String motivo, OffsetDateTime fecha) {
        return normalizarMotivo(motivo)
                .flatMap(valor -> estado.retener().map(nuevo -> cambiarEstado(nuevo, valor, fecha)));
    }

    public Result<EstadoCambiado, ErrorBilletera> reactivar(OffsetDateTime fecha) {
        return estado.reactivar().map(nuevo -> cambiarEstado(nuevo, null, fecha));
    }

    private EstadoCambiado cambiarEstado(EstadoBilletera nuevo, String motivo, OffsetDateTime fecha) {
        var evento = new EstadoCambiado(id, estado.nombre(), nuevo.nombre(), motivo, Objects.requireNonNull(fecha));
        estado = nuevo;
        motivoEstado = motivo;
        return evento;
    }

    private static Result<String, ErrorBilletera> normalizarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) return Result.err(new ErrorBilletera.MotivoEstadoInvalido());
        var texto = motivo.strip();
        return Result.ok(texto.substring(0, Math.min(MAXIMO_CARACTERES_MOTIVO, texto.length())));
    }

    public Long getId() {
        return id;
    }

    public UsuarioId getUsuarioId() {
        return usuarioId;
    }

    public Celular getCelular() {
        return celular;
    }

    public String getNombreNegocio() {
        return nombreNegocio;
    }

    public Dinero getSaldo() {
        return saldo;
    }

    public Dinero getSaldoApartado() {
        return saldoApartado;
    }

    public String getEstado() {
        return estado.nombre();
    }

    public String getMotivoBloqueo() {
        return motivoEstado;
    }

    public OffsetDateTime getFechaApertura() {
        return fechaApertura;
    }

    public Long getVersion() {
        return version;
    }
}
