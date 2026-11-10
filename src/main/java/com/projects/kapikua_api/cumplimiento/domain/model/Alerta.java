package com.projects.kapikua_api.cumplimiento.domain.model;

import com.projects.kapikua_api.cumplimiento.domain.error.ErrorCumplimiento;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

import java.time.OffsetDateTime;
import java.util.Objects;

public final class Alerta {
    private final Long id;
    private final Long billeteraId;
    private final Long transaccionId;
    private final ReglaAlerta regla;
    private final String detalle;
    private final OffsetDateTime fecha;
    private EstadoAlerta estado;
    private String comentario;
    private UsuarioId revisadoPor;
    private OffsetDateTime fechaRevision;

    private Alerta(Long id, Long billeteraId, Long transaccionId, ReglaAlerta regla, String detalle, OffsetDateTime fecha,
                   EstadoAlerta estado, String comentario, UsuarioId revisadoPor, OffsetDateTime fechaRevision) {
        this.id = id;
        this.billeteraId = Objects.requireNonNull(billeteraId);
        this.transaccionId = Objects.requireNonNull(transaccionId);
        this.regla = Objects.requireNonNull(regla);
        this.detalle = Objects.requireNonNull(detalle);
        this.fecha = Objects.requireNonNull(fecha);
        this.estado = Objects.requireNonNull(estado);
        this.comentario = comentario;
        this.revisadoPor = revisadoPor;
        this.fechaRevision = fechaRevision;
    }

    public static Alerta nueva(Long billeteraId, Long transaccionId, ReglaAlerta regla, String detalle, OffsetDateTime fecha) {
        return new Alerta(null, billeteraId, transaccionId, regla, detalle, fecha, EstadoAlerta.PENDIENTE, null, null, null);
    }

    public static Alerta reconstituir(Long id, Long billeteraId, Long transaccionId, ReglaAlerta regla, String detalle,
                                      OffsetDateTime fecha, EstadoAlerta estado, String comentario, UsuarioId revisadoPor,
                                      OffsetDateTime fechaRevision) {
        return new Alerta(id, billeteraId, transaccionId, regla, detalle, fecha, estado, comentario, revisadoPor, fechaRevision);
    }

    public Result<Void, ErrorCumplimiento> revisar(EstadoAlerta decision, String comentario, UsuarioId administrador,
                                                   OffsetDateTime ahora) {
        if (estado != EstadoAlerta.PENDIENTE) return Result.err(new ErrorCumplimiento.AlertaYaRevisada());
        if (decision == null || decision == EstadoAlerta.PENDIENTE) return Result.err(new ErrorCumplimiento.DecisionInvalida());
        var texto = comentario == null ? "" : comentario.strip();
        if (texto.length() < 5 || texto.length() > 255) return Result.err(new ErrorCumplimiento.ComentarioInvalido());
        this.estado = decision;
        this.comentario = texto;
        this.revisadoPor = Objects.requireNonNull(administrador);
        this.fechaRevision = ahora;
        return Result.ok();
    }

    public boolean estaPendiente() { return estado == EstadoAlerta.PENDIENTE; }

    public Long getId() { return id; }
    public Long getBilleteraId() { return billeteraId; }
    public Long getTransaccionId() { return transaccionId; }
    public ReglaAlerta getRegla() { return regla; }
    public String getDetalle() { return detalle; }
    public OffsetDateTime getFecha() { return fecha; }
    public EstadoAlerta getEstado() { return estado; }
    public String getComentario() { return comentario; }
    public UsuarioId getRevisadoPor() { return revisadoPor; }
    public OffsetDateTime getFechaRevision() { return fechaRevision; }
}
