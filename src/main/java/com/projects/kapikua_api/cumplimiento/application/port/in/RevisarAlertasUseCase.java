package com.projects.kapikua_api.cumplimiento.application.port.in;

import com.projects.kapikua_api.cumplimiento.domain.error.ErrorCumplimiento;
import com.projects.kapikua_api.cumplimiento.domain.model.EstadoAlerta;
import com.projects.kapikua_api.cumplimiento.domain.model.OperacionObservada;
import com.projects.kapikua_api.cumplimiento.domain.model.ReglaAlerta;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

import java.time.OffsetDateTime;
import java.util.List;

public interface RevisarAlertasUseCase {
    List<ResumenAlerta> pendientes();

    List<ResumenAlerta> revisadas(int limite);

    Result<DetalleAlerta, ErrorCumplimiento> detalle(Long alertaId);

    Result<Void, ErrorCumplimiento> revisar(Long alertaId, UsuarioId administrador, EstadoAlerta decision,
                                            String comentario, boolean bloquear, String motivoBloqueo);

    record Cliente(String nombre, String dniEnmascarado) {}

    record ResumenAlerta(Long id, ReglaAlerta regla, Cliente cliente, String detalle, OffsetDateTime fecha,
                         EstadoAlerta estado) {}

    record DetalleAlerta(ResumenAlerta alerta, Long transaccionId, List<OperacionObservada> operaciones,
                         String comentario, String revisadoPor, OffsetDateTime fechaRevision,
                         boolean billeteraBloqueada) {}
}
