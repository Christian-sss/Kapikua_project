package com.projects.kapikua_api.cumplimiento.application.port.in;

import com.projects.kapikua_api.cumplimiento.domain.error.ErrorCumplimiento;
import com.projects.kapikua_api.cumplimiento.domain.model.EstadoAlerta;
import com.projects.kapikua_api.cumplimiento.domain.model.OperacionObservada;
import com.projects.kapikua_api.cumplimiento.domain.model.ReglaAlerta;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

import java.time.OffsetDateTime;
import java.util.List;

/** CU-14 Revisar alertas. El cliente se muestra con el DNI enmascarado (RN-30). */
public interface RevisarAlertasUseCase {

    List<ResumenAlerta> pendientes();

    /** Las últimas revisadas, para consulta (la decisión no se puede cambiar). */
    List<ResumenAlerta> revisadas(int limite);

    Result<DetalleAlerta, ErrorCumplimiento> detalle(Long alertaId);

    /**
     * @param bloquear   solo con decisión ESCALADA (CU-14, 4a)
     * @param motivoBloqueo requerido si {@code bloquear}
     */
    Result<Void, ErrorCumplimiento> revisar(Long alertaId, UsuarioId administrador, EstadoAlerta decision,
                                            String comentario, boolean bloquear, String motivoBloqueo);

    record Cliente(String nombre, String dniEnmascarado) {}

    record ResumenAlerta(Long id, ReglaAlerta regla, Cliente cliente, String detalle, OffsetDateTime fecha,
                         EstadoAlerta estado) {}

    record DetalleAlerta(ResumenAlerta alerta, Long transaccionId, List<OperacionObservada> operaciones,
                         String comentario, String revisadoPor, OffsetDateTime fechaRevision,
                         boolean billeteraBloqueada) {}
}
