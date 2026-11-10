package com.projects.kapikua_api.billetera.domain.model;

import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Raíz del agregado Billetera. Es Java puro: el adaptador de persistencia la convierte a su entidad JPA,
 * así que un cambio en memoria solo se guarda al llamar al repositorio.
 * {@code saldoApartado} es parte de {@code saldo} (lo retenido para la cuota): no se puede gastar.
 */
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

    /** CU-01, paso 8: billetera ACTIVA con saldo cero. */
    public static Billetera abrir(UsuarioId usuarioId, Celular celular, String nombreNegocio, OffsetDateTime ahora) {
        return new Billetera(null, usuarioId, celular, nombreNegocio, Dinero.CERO, Dinero.CERO,
                EstadoBilletera.ACTIVA, null, ahora, null);
    }

    /** Solo para el adaptador de persistencia. */
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

    /** Solo una billetera con nombre de negocio puede recibir pagos a negocio (CU-06). */
    public boolean esNegocio() {
        return nombreNegocio != null;
    }

    /** Lo que todavía puede entrar sin superar el saldo máximo (RN-02). */
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

    /**
     * RN-02: ninguna entrada puede dejar el saldo por encima del máximo.
     * {@code esDestinoAjeno} cambia el mensaje para no revelar el saldo de otra persona.
     */
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

    /**
     * RN-22: aparta para la cuota hasta {@code monto}, sin pasar del saldo disponible.
     * Lo apartado sigue siendo parte del saldo, pero no se puede gastar. Devuelve lo que se apartó.
     */
    public Dinero apartar(Dinero monto) {
        var posible = monto.menorQue(saldoDisponible()) ? monto : saldoDisponible();
        saldoApartado = saldoApartado.sumar(posible);
        return posible;
    }

    /** Lo apartado vuelve a estar disponible (cargo revocado o cuota ya cubierta). */
    public void liberarApartado(Dinero monto) {
        saldoApartado = saldoApartado.mayorQue(monto) ? saldoApartado.restar(monto) : Dinero.CERO;
    }

    /** Paga con lo apartado: sale del saldo y del apartado a la vez. */
    public void consumirApartado(Dinero monto) {
        if (monto.mayorQue(saldoApartado)) throw new IllegalArgumentException("No hay tanto apartado");
        saldoApartado = saldoApartado.restar(monto);
        saldo = saldo.restar(monto);
    }

    /**
     * CU-14, 4a: bloqueo por cumplimiento al escalar una alerta. Una billetera bloqueada no envía ni recibe dinero.
     * Si ya estaba bloqueada, conserva el primer motivo.
     */
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
