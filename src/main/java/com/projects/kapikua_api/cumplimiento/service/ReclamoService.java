package com.projects.kapikua_api.cumplimiento.service;

import com.projects.kapikua_api.cumplimiento.client.ContextosCumplimiento;
import com.projects.kapikua_api.cumplimiento.dto.Cliente;
import com.projects.kapikua_api.cumplimiento.dto.VistaReclamo;
import com.projects.kapikua_api.cumplimiento.error.ErrorCumplimiento;
import com.projects.kapikua_api.cumplimiento.model.MotivoReclamo;
import com.projects.kapikua_api.cumplimiento.model.Reclamo;
import com.projects.kapikua_api.cumplimiento.repository.ReclamoRepository;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Reclamos de clientes con plazo de respuesta en días hábiles. */
@Service
public class ReclamoService {
    private final ReclamoRepository reclamos;
    private final ContextosCumplimiento contextos;
    private final Clock clock;

    public ReclamoService(ReclamoRepository reclamos, ContextosCumplimiento contextos, Clock clock) {
        this.reclamos = reclamos;
        this.contextos = contextos;
        this.clock = clock;
    }

    public Result<VistaReclamo, ErrorCumplimiento> registrar(UsuarioId usuarioId, MotivoReclamo motivo, String detalle,
                                                             Long transaccionId) {
        if (transaccionId != null && !contextos.operacionDelUsuario(usuarioId, transaccionId)) {
            return Result.err(new ErrorCumplimiento.OperacionAjena());
        }
        return Reclamo.registrar(usuarioId, motivo, detalle, transaccionId, OffsetDateTime.now(clock))
                .map(reclamos::registrar)
                .map(r -> vista(r, null));
    }

    public List<VistaReclamo> deUsuario(UsuarioId usuarioId) {
        return reclamos.deUsuario(usuarioId).stream().map(r -> vista(r, null)).toList();
    }

    public Result<VistaReclamo, ErrorCumplimiento> delUsuario(UsuarioId usuarioId, Long reclamoId) {
        return reclamos.porId(reclamoId).filter(r -> r.getUsuarioId().equals(usuarioId))
                .<Result<VistaReclamo, ErrorCumplimiento>>map(r -> Result.ok(vista(r, null)))
                .orElseGet(() -> Result.err(new ErrorCumplimiento.ReclamoNoEncontrado()));
    }

    public List<VistaReclamo> pendientes() {
        return reclamos.pendientes().stream().map(r -> vista(r, cliente(r))).toList();
    }

    public List<VistaReclamo> atendidos(int limite) {
        return reclamos.atendidos(limite).stream().map(r -> vista(r, cliente(r))).toList();
    }

    public Result<VistaReclamo, ErrorCumplimiento> detalle(Long reclamoId) {
        return reclamos.porId(reclamoId)
                .<Result<VistaReclamo, ErrorCumplimiento>>map(r -> Result.ok(vista(r, cliente(r))))
                .orElseGet(() -> Result.err(new ErrorCumplimiento.ReclamoNoEncontrado()));
    }

    public Result<Void, ErrorCumplimiento> responder(Long reclamoId, UsuarioId administrador, String respuesta) {
        var reclamo = reclamos.porId(reclamoId);
        if (reclamo.isEmpty()) return Result.err(new ErrorCumplimiento.ReclamoNoEncontrado());
        return reclamo.get().responder(respuesta, administrador, OffsetDateTime.now(clock))
                .flatMap(v -> reclamos.guardarRespuesta(reclamo.get()) ? Result.<ErrorCumplimiento>ok()
                        : Result.err(new ErrorCumplimiento.ReclamoYaAtendido()));
    }

    public Result<Void, ErrorCumplimiento> prorrogar(Long reclamoId, String justificacion) {
        var reclamo = reclamos.porId(reclamoId);
        if (reclamo.isEmpty()) return Result.err(new ErrorCumplimiento.ReclamoNoEncontrado());
        return reclamo.get().prorrogar(justificacion)
                .flatMap(v -> reclamos.guardarProrroga(reclamo.get()) ? Result.<ErrorCumplimiento>ok()
                        : Result.err(new ErrorCumplimiento.OperacionConcurrente()));
    }

    private Cliente cliente(Reclamo r) {
        return contextos.cliente(r.getUsuarioId()).map(c -> new Cliente(c.nombre(), c.dniEnmascarado()))
                .orElse(new Cliente("—", "—"));
    }

    private VistaReclamo vista(Reclamo r, Cliente cliente) {
        var hoy = LocalDate.now(clock);
        return new VistaReclamo(r.getId(), r.getCodigo(), r.getMotivo(), r.getDetalle(), r.getEstado(),
                r.getFechaRegistro(), r.getFechaLimite(), r.diasHabilesRestantes(hoy), r.cercaDelLimite(hoy),
                r.fueraDePlazo(hoy), r.getProrroga(), r.getRespuesta(), r.getFechaRespuesta(), cliente);
    }
}
