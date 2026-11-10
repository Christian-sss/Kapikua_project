package com.projects.kapikua_api.cumplimiento.application.port.in;

import com.projects.kapikua_api.cumplimiento.domain.error.ErrorCumplimiento;
import com.projects.kapikua_api.cumplimiento.domain.model.EstadoReclamo;
import com.projects.kapikua_api.cumplimiento.domain.model.MotivoReclamo;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public interface ReclamosUseCase {
    Result<VistaReclamo, ErrorCumplimiento> registrar(UsuarioId usuarioId, MotivoReclamo motivo, String detalle,
                                                      Long transaccionId);

    List<VistaReclamo> deUsuario(UsuarioId usuarioId);

    Result<VistaReclamo, ErrorCumplimiento> delUsuario(UsuarioId usuarioId, Long reclamoId);

    List<VistaReclamo> pendientes();

    List<VistaReclamo> atendidos(int limite);

    Result<VistaReclamo, ErrorCumplimiento> detalle(Long reclamoId);

    Result<Void, ErrorCumplimiento> responder(Long reclamoId, UsuarioId administrador, String respuesta);

    Result<Void, ErrorCumplimiento> prorrogar(Long reclamoId, String justificacion);

    record VistaReclamo(Long id, String codigo, MotivoReclamo motivo, String detalle, EstadoReclamo estado,
                        OffsetDateTime fechaRegistro, LocalDate fechaLimite, int diasHabilesRestantes,
                        boolean cercaDelLimite, boolean fueraDePlazo, String prorroga, String respuesta,
                        OffsetDateTime fechaRespuesta, RevisarAlertasUseCase.Cliente cliente) {}
}
