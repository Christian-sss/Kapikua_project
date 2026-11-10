package com.projects.kapikua_api.credito.domain.model;

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

    public static Aval reconstituir(Long id, UsuarioId deudor, UsuarioId aval, EstadoAval estado, Integer porcentaje,
                                    OffsetDateTime fechaInvitacion, OffsetDateTime fechaRespuesta, String dispositivo,
                                    OffsetDateTime fechaAviso, OffsetDateTime fechaActivacion,
                                    OffsetDateTime fechaLiberacion) {
        return new Aval(id, deudor, aval, estado, porcentaje, fechaInvitacion, fechaRespuesta, dispositivo,
                fechaAviso, fechaActivacion, fechaLiberacion);
    }

    public OffsetDateTime venceEn(Duration plazo, OffsetDateTime finVigenciaOferta) {
        var limite = fechaInvitacion.plus(plazo);
        return limite.isBefore(finVigenciaOferta) ? limite : finVigenciaOferta;
    }

    void aceptar(int porcentajeRetencion, String dispositivo, OffsetDateTime ahora) {
        if (estado != EstadoAval.PENDIENTE) throw new IllegalStateException("La invitación no está pendiente");
        estado = EstadoAval.ACEPTADO;
        porcentajeRetencionAutorizado = porcentajeRetencion;
        dispositivoAceptacion = dispositivo == null || dispositivo.isBlank() ? "desconocido"
                : dispositivo.substring(0, Math.min(150, dispositivo.length()));
        fechaRespuesta = ahora;
    }

    void rechazar(OffsetDateTime cuando) {
        if (estado != EstadoAval.PENDIENTE) throw new IllegalStateException("La invitación no está pendiente");
        estado = EstadoAval.RECHAZADO;
        fechaRespuesta = cuando.isBefore(fechaInvitacion) ? fechaInvitacion : cuando;
    }

    boolean avisar(OffsetDateTime ahora) {
        if (fechaAviso != null || (estado != EstadoAval.ACEPTADO && estado != EstadoAval.ACTIVADO)) return false;
        fechaAviso = ahora;
        return true;
    }

    boolean activar(OffsetDateTime ahora) {
        if (estado != EstadoAval.ACEPTADO) return false;
        if (fechaAviso == null) fechaAviso = ahora;
        estado = EstadoAval.ACTIVADO;
        fechaActivacion = ahora;
        return true;
    }

    void liberar(OffsetDateTime ahora) {
        if (estado == EstadoAval.ACEPTADO || estado == EstadoAval.ACTIVADO) {
            estado = EstadoAval.LIBERADO;
            fechaLiberacion = ahora;
        }
    }

    public boolean esPendiente() { return estado == EstadoAval.PENDIENTE; }

    public boolean estaActivado() { return estado == EstadoAval.ACTIVADO; }

    public Long getId() { return id; }
    public UsuarioId getUsuarioDeudorId() { return usuarioDeudorId; }
    public UsuarioId getUsuarioAvalId() { return usuarioAvalId; }
    public EstadoAval getEstado() { return estado; }
    public Integer getPorcentajeRetencionAutorizado() { return porcentajeRetencionAutorizado; }
    public OffsetDateTime getFechaInvitacion() { return fechaInvitacion; }
    public OffsetDateTime getFechaRespuesta() { return fechaRespuesta; }
    public String getDispositivoAceptacion() { return dispositivoAceptacion; }
    public OffsetDateTime getFechaAviso() { return fechaAviso; }
    public OffsetDateTime getFechaActivacion() { return fechaActivacion; }
    public OffsetDateTime getFechaLiberacion() { return fechaLiberacion; }
}
