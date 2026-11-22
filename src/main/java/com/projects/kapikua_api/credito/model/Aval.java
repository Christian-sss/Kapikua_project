package com.projects.kapikua_api.credito.model;

import com.projects.kapikua_api.credito.error.ErrorCredito;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;

public final class Aval {
    private final Long id;
    private EstadoAval estado;
    private Invitacion invitacion;
    private Respuesta respuesta;
    private CicloGarantia cicloGarantia;

    private Aval(Long id, EstadoAval estado, Invitacion invitacion, Respuesta respuesta, CicloGarantia cicloGarantia) {
        this.id = id;
        this.estado = Objects.requireNonNull(estado);
        this.invitacion = Objects.requireNonNull(invitacion);
        this.respuesta = Objects.requireNonNull(respuesta);
        this.cicloGarantia = Objects.requireNonNull(cicloGarantia);
    }

    public static Aval invitar(UsuarioId deudor, UsuarioId aval, OffsetDateTime ahora) {
        return new Aval(
                null,
                EstadoAval.PENDIENTE,
                new Invitacion(deudor, aval, ahora),
                Respuesta.NINGUNA,
                CicloGarantia.NINGUNO
        );
    }

    public static Aval reconstituir(Long id, String estado, Invitacion invitacion, Respuesta respuesta, CicloGarantia cicloGarantia) {
        return new Aval(id, EstadoAval.desdeNombre(estado), invitacion, respuesta, cicloGarantia);
    }


    public OffsetDateTime venceEn(Duration plazo, OffsetDateTime finVigenciaOferta) {
        var limite = invitacion.fechaInvitacion().plus(plazo);
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

        respuesta = new Respuesta(ahora, dispositivoNormalizado, porcentajeRetencion);

        return cambio;
    }

    CambioEstado aplicarRechazo(OffsetDateTime cuando) {
        var fecha = cuando.isBefore(invitacion.fechaInvitacion()) ? invitacion.fechaInvitacion() : cuando;
        var cambio = cambiarEstado(EstadoAval.RECHAZADO, "Invitación rechazada", fecha);
        respuesta = new Respuesta(fecha, null, null);

        return cambio;
    }

    boolean registrarAviso(OffsetDateTime ahora) {
        if (cicloGarantia.fechaAviso() != null) return false;
        cicloGarantia = cicloGarantia.conAviso(Objects.requireNonNull(ahora));
        return true;
    }

    CambioEstado aplicarActivacion(OffsetDateTime ahora) {
        var cambio = cambiarEstado(EstadoAval.ACTIVADO, "Garantía activada", ahora);
        registrarAviso(ahora);
        cicloGarantia = cicloGarantia.conActivacion(ahora);
        return cambio;
    }

    CambioEstado aplicarLiberacion(OffsetDateTime ahora) {
        var cambio = cambiarEstado(EstadoAval.LIBERADO, "Garantía liberada", ahora);
        cicloGarantia = cicloGarantia.conLiberacion(ahora);
        return cambio;
    }

    private CambioEstado cambiarEstado(EstadoAval nuevo, String motivo, OffsetDateTime fecha) {
        var cambio = new CambioEstado(estado.nombre(), nuevo.nombre(), motivo, Objects.requireNonNull(fecha));
        estado = nuevo;
        return cambio;
    }

    public boolean esPendiente() {
        return estado.esPendiente();
    }

    public boolean estaAceptado() {
        return estado.estaAceptado();
    }

    public boolean estaActivado() {
        return estado.estaActivado();
    }

    public boolean tieneGarantia() {
        return estado.tieneGarantia();
    }

    public boolean enCurso() {
        return estado.enCurso();
    }

    public Long getId() {
        return id;
    }

    public String getEstado() {
        return estado.nombre();
    }

    public UsuarioId getUsuarioDeudorId() {
        return invitacion.usuarioDeudorId();
    }

    public UsuarioId getUsuarioAvalId() {
        return invitacion.usuarioAvalId();
    }

    public Integer getPorcentajeRetencionAutorizado() {
        return respuesta.porcentajeRetencionAutorizado();
    }

    public OffsetDateTime getFechaInvitacion() {
        return invitacion.fechaInvitacion();
    }

    public OffsetDateTime getFechaRespuesta() {
        return respuesta.fechaRespuesta();
    }

    public String getDispositivoAceptacion() {
        return respuesta.dispositivoAceptacion();
    }

    public OffsetDateTime getFechaAviso() {
        return cicloGarantia.fechaAviso();
    }

    public OffsetDateTime getFechaActivacion() {
        return cicloGarantia.fechaActivacion();
    }

    public OffsetDateTime getFechaLiberacion() {
        return cicloGarantia.fechaLiberacion();
    }
}
