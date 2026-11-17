package com.projects.kapikua_api.credito.domain.model;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;

public final class Aval {
    private final Long id;
    private final UsuarioId usuarioDeudorId;
    private final UsuarioId usuarioAvalId;
    private EstadoAval estado;
    private Integer porcentajeRetencionAutorizado;
    private final OffsetDateTime fechaInvitacion;
    private OffsetDateTime fechaRespuesta;
    private String dispositivoAceptacion;
    private OffsetDateTime fechaAviso;
    private OffsetDateTime fechaActivacion;
    private OffsetDateTime fechaLiberacion;

    private Aval(Long id, UsuarioId usuarioDeudorId, UsuarioId usuarioAvalId, EstadoAval estado,
                 Integer porcentajeRetencionAutorizado, OffsetDateTime fechaInvitacion, OffsetDateTime fechaRespuesta,
                 String dispositivoAceptacion, OffsetDateTime fechaAviso, OffsetDateTime fechaActivacion,
                 OffsetDateTime fechaLiberacion) {
        this.id = id;
        this.usuarioDeudorId = Objects.requireNonNull(usuarioDeudorId);
        this.usuarioAvalId = Objects.requireNonNull(usuarioAvalId);
        if (usuarioDeudorId.equals(usuarioAvalId)) throw new IllegalArgumentException("El aval no puede ser el deudor");
        this.estado = Objects.requireNonNull(estado);
        this.porcentajeRetencionAutorizado = porcentajeRetencionAutorizado;
        this.fechaInvitacion = Objects.requireNonNull(fechaInvitacion);
        this.fechaRespuesta = fechaRespuesta;
        this.dispositivoAceptacion = dispositivoAceptacion;
        this.fechaAviso = fechaAviso;
        this.fechaActivacion = fechaActivacion;
        this.fechaLiberacion = fechaLiberacion;
    }

    static Aval invitar(UsuarioId deudor, UsuarioId aval, OffsetDateTime ahora) {
        return new Aval(null, deudor, aval, EstadoAval.PENDIENTE, null, ahora, null, null, null, null, null);
    }

    public static Aval reconstituir(Long id, UsuarioId deudor, UsuarioId aval, String estado, Integer porcentaje,
                                    OffsetDateTime fechaInvitacion, OffsetDateTime fechaRespuesta, String dispositivo,
                                    OffsetDateTime fechaAviso, OffsetDateTime fechaActivacion,
                                    OffsetDateTime fechaLiberacion) {
        return new Aval(id, deudor, aval, EstadoAval.desdeNombre(estado), porcentaje, fechaInvitacion, fechaRespuesta,
                dispositivo, fechaAviso, fechaActivacion, fechaLiberacion);
    }

    public OffsetDateTime venceEn(Duration plazo, OffsetDateTime finVigenciaOferta) {
        var limite = fechaInvitacion.plus(plazo);
        return limite.isBefore(finVigenciaOferta) ? limite : finVigenciaOferta;
    }

    Result<CambioEstado, ErrorCredito> aceptar(int porcentajeRetencion, String dispositivo, OffsetDateTime ahora) {
        return estado.aceptar(this, porcentajeRetencion, dispositivo, ahora);
    }

    Result<CambioEstado, ErrorCredito> rechazar(OffsetDateTime cuando) {
        return estado.rechazar(this, cuando);
    }

    boolean avisar(OffsetDateTime ahora) {
        return estado.avisar(this, ahora);
    }

    Result<CambioEstado, ErrorCredito> activar(OffsetDateTime ahora) {
        return estado.activar(this, ahora);
    }

    Result<CambioEstado, ErrorCredito> liberar(OffsetDateTime ahora) {
        return estado.liberar(this, ahora);
    }

    CambioEstado aplicarAceptacion(int porcentajeRetencion, String dispositivo, OffsetDateTime ahora) {
        var dispositivoNormalizado = dispositivo == null || dispositivo.isBlank() ? "desconocido"
                : dispositivo.substring(0, Math.min(150, dispositivo.length()));
        var cambio = cambiarEstado(EstadoAval.ACEPTADO, "Invitación aceptada", ahora);
        porcentajeRetencionAutorizado = porcentajeRetencion;
        dispositivoAceptacion = dispositivoNormalizado;
        fechaRespuesta = ahora;
        return cambio;
    }

    CambioEstado aplicarRechazo(OffsetDateTime cuando) {
        var fecha = cuando.isBefore(fechaInvitacion) ? fechaInvitacion : cuando;
        var cambio = cambiarEstado(EstadoAval.RECHAZADO, "Invitación rechazada", fecha);
        fechaRespuesta = fecha;
        return cambio;
    }

    boolean registrarAviso(OffsetDateTime ahora) {
        if (fechaAviso != null) return false;
        fechaAviso = Objects.requireNonNull(ahora);
        return true;
    }

    CambioEstado aplicarActivacion(OffsetDateTime ahora) {
        var cambio = cambiarEstado(EstadoAval.ACTIVADO, "Garantía activada", ahora);
        registrarAviso(ahora);
        fechaActivacion = ahora;
        return cambio;
    }

    CambioEstado aplicarLiberacion(OffsetDateTime ahora) {
        var cambio = cambiarEstado(EstadoAval.LIBERADO, "Garantía liberada", ahora);
        fechaLiberacion = ahora;
        return cambio;
    }

    private CambioEstado cambiarEstado(EstadoAval nuevo, String motivo, OffsetDateTime fecha) {
        var cambio = new CambioEstado(estado.nombre(), nuevo.nombre(), motivo, Objects.requireNonNull(fecha));
        estado = nuevo;
        return cambio;
    }

    public boolean esPendiente() { return estado.esPendiente(); }

    public boolean estaAceptado() { return estado.estaAceptado(); }

    public boolean estaActivado() { return estado.estaActivado(); }

    public boolean tieneGarantia() { return estado.tieneGarantia(); }

    public boolean enCurso() { return estado.enCurso(); }

    public Long getId() { return id; }
    public UsuarioId getUsuarioDeudorId() { return usuarioDeudorId; }
    public UsuarioId getUsuarioAvalId() { return usuarioAvalId; }
    public String getEstado() { return estado.nombre(); }
    public Integer getPorcentajeRetencionAutorizado() { return porcentajeRetencionAutorizado; }
    public OffsetDateTime getFechaInvitacion() { return fechaInvitacion; }
    public OffsetDateTime getFechaRespuesta() { return fechaRespuesta; }
    public String getDispositivoAceptacion() { return dispositivoAceptacion; }
    public OffsetDateTime getFechaAviso() { return fechaAviso; }
    public OffsetDateTime getFechaActivacion() { return fechaActivacion; }
    public OffsetDateTime getFechaLiberacion() { return fechaLiberacion; }
}
