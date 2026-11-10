package com.projects.kapikua_api.cumplimiento.domain.model;

import com.projects.kapikua_api.cumplimiento.domain.service.CalendarioHabil;

import com.projects.kapikua_api.cumplimiento.domain.error.ErrorCumplimiento;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

public final class Reclamo {
    public static final int PLAZO_DIAS_HABILES = 15;
    public static final int DIAS_ALERTA_PLAZO = 3;
    static final int MAX_DETALLE = 500;

    private final Long id;
    private final String codigo;
    private final UsuarioId usuarioId;
    private final MotivoReclamo motivo;
    private final String detalle;
    private final OffsetDateTime fechaRegistro;
    private LocalDate fechaLimite;
    private EstadoReclamo estado;
    private String prorroga;
    private String respuesta;
    private UsuarioId respondidoPor;
    private OffsetDateTime fechaRespuesta;

    private Reclamo(Long id, String codigo, UsuarioId usuarioId, MotivoReclamo motivo, String detalle,
                    OffsetDateTime fechaRegistro, LocalDate fechaLimite, EstadoReclamo estado, String prorroga,
                    String respuesta, UsuarioId respondidoPor, OffsetDateTime fechaRespuesta) {
        this.id = id;
        this.codigo = codigo;
        this.usuarioId = Objects.requireNonNull(usuarioId);
        this.motivo = Objects.requireNonNull(motivo);
        this.detalle = Objects.requireNonNull(detalle);
        this.fechaRegistro = Objects.requireNonNull(fechaRegistro);
        this.fechaLimite = Objects.requireNonNull(fechaLimite);
        this.estado = Objects.requireNonNull(estado);
        this.prorroga = prorroga;
        this.respuesta = respuesta;
        this.respondidoPor = respondidoPor;
        this.fechaRespuesta = fechaRespuesta;
    }

    public static Result<Reclamo, ErrorCumplimiento> registrar(UsuarioId usuarioId, MotivoReclamo motivo, String detalle,
                                                               Long transaccionId, OffsetDateTime ahora) {
        if (motivo == null) return Result.err(new ErrorCumplimiento.MotivoRequerido());
        var texto = detalle == null ? "" : detalle.strip();
        var prefijo = transaccionId == null ? "" : "Operación N° " + transaccionId + ": ";
        if (texto.length() < 10 || prefijo.length() + texto.length() > MAX_DETALLE) {
            return Result.err(new ErrorCumplimiento.DetalleInvalido(MAX_DETALLE - prefijo.length()));
        }
        var limite = CalendarioHabil.sumarDiasHabiles(ahora.toLocalDate(), PLAZO_DIAS_HABILES);
        return Result.ok(new Reclamo(null, null, usuarioId, motivo, prefijo + texto, ahora, limite,
                EstadoReclamo.REGISTRADO, null, null, null, null));
    }

    public static Reclamo reconstituir(Long id, String codigo, UsuarioId usuarioId, MotivoReclamo motivo, String detalle,
                                       OffsetDateTime fechaRegistro, LocalDate fechaLimite, EstadoReclamo estado,
                                       String prorroga, String respuesta, UsuarioId respondidoPor,
                                       OffsetDateTime fechaRespuesta) {
        return new Reclamo(id, codigo, usuarioId, motivo, detalle, fechaRegistro, fechaLimite, estado, prorroga,
                respuesta, respondidoPor, fechaRespuesta);
    }

    public Result<Void, ErrorCumplimiento> responder(String texto, UsuarioId administrador, OffsetDateTime ahora) {
        if (estado == EstadoReclamo.ATENDIDO) return Result.err(new ErrorCumplimiento.ReclamoYaAtendido());
        var limpio = texto == null ? "" : texto.strip();
        if (limpio.length() < 10 || limpio.length() > MAX_DETALLE) return Result.err(new ErrorCumplimiento.RespuestaInvalida());
        this.respuesta = limpio;
        this.respondidoPor = Objects.requireNonNull(administrador);
        this.fechaRespuesta = ahora;
        this.estado = EstadoReclamo.ATENDIDO;
        return Result.ok();
    }

    public Result<Void, ErrorCumplimiento> prorrogar(String justificacion) {
        if (estado == EstadoReclamo.ATENDIDO) return Result.err(new ErrorCumplimiento.ReclamoYaAtendido());
        if (prorroga != null) return Result.err(new ErrorCumplimiento.ProrrogaYaRegistrada());
        var limpio = justificacion == null ? "" : justificacion.strip();
        if (limpio.length() < 10 || limpio.length() > 255) return Result.err(new ErrorCumplimiento.JustificacionInvalida());
        this.prorroga = limpio;
        this.fechaLimite = CalendarioHabil.sumarDiasHabiles(fechaLimite, PLAZO_DIAS_HABILES);
        return Result.ok();
    }

    public boolean estaPendiente() { return estado == EstadoReclamo.REGISTRADO; }

    public int diasHabilesRestantes(LocalDate hoy) {
        return CalendarioHabil.diasHabilesHasta(hoy, fechaLimite);
    }

    public boolean cercaDelLimite(LocalDate hoy) {
        return estaPendiente() && diasHabilesRestantes(hoy) <= DIAS_ALERTA_PLAZO;
    }

    public boolean fueraDePlazo(LocalDate hoy) {
        return estaPendiente() && hoy.isAfter(fechaLimite);
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public UsuarioId getUsuarioId() { return usuarioId; }
    public MotivoReclamo getMotivo() { return motivo; }
    public String getDetalle() { return detalle; }
    public OffsetDateTime getFechaRegistro() { return fechaRegistro; }
    public LocalDate getFechaLimite() { return fechaLimite; }
    public EstadoReclamo getEstado() { return estado; }
    public String getProrroga() { return prorroga; }
    public String getRespuesta() { return respuesta; }
    public UsuarioId getRespondidoPor() { return respondidoPor; }
    public OffsetDateTime getFechaRespuesta() { return fechaRespuesta; }
}
