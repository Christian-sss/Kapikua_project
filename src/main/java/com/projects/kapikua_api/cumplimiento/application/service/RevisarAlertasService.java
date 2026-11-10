package com.projects.kapikua_api.cumplimiento.application.service;

import com.projects.kapikua_api.cumplimiento.application.port.in.RevisarAlertasUseCase;
import com.projects.kapikua_api.cumplimiento.application.port.out.AlertaRepositoryPort;
import com.projects.kapikua_api.cumplimiento.application.port.out.ContextosCumplimiento;
import com.projects.kapikua_api.cumplimiento.domain.error.ErrorCumplimiento;
import com.projects.kapikua_api.cumplimiento.domain.model.Alerta;
import com.projects.kapikua_api.cumplimiento.domain.model.EstadoAlerta;
import com.projects.kapikua_api.cumplimiento.domain.service.ReglasOperacionesInusuales;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.puerto.EjecutorTransaccional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class RevisarAlertasService implements RevisarAlertasUseCase {

    private static final Cliente DESCONOCIDO = new Cliente("—", "—");

    private final AlertaRepositoryPort alertas;
    private final ContextosCumplimiento contextos;
    private final EjecutorTransaccional ejecutor;
    private final Clock clock;

    public RevisarAlertasService(AlertaRepositoryPort alertas, ContextosCumplimiento contextos,
                                 EjecutorTransaccional ejecutor, Clock clock) {
        this.alertas = alertas;
        this.contextos = contextos;
        this.ejecutor = ejecutor;
        this.clock = clock;
    }

    @Override
    public List<ResumenAlerta> pendientes() {
        return alertas.pendientes().stream().map(this::resumen).toList();
    }

    @Override
    public List<ResumenAlerta> revisadas(int limite) {
        return alertas.revisadas(limite).stream().map(this::resumen).toList();
    }

    @Override
    public Result<DetalleAlerta, ErrorCumplimiento> detalle(Long alertaId) {
        var alerta = alertas.porId(alertaId);
        if (alerta.isEmpty()) return Result.err(new ErrorCumplimiento.AlertaNoEncontrada());
        var a = alerta.get();
        // Las operaciones que vio la regla: las 24 horas anteriores a la alerta.
        var operaciones = contextos.operaciones(a.getBilleteraId(),
                a.getFecha().minus(ReglasOperacionesInusuales.VENTANA_FRACCIONAMIENTO), a.getFecha().plusSeconds(1));
        var revisor = a.getRevisadoPor() == null ? null
                : contextos.cliente(a.getRevisadoPor()).map(ContextosCumplimiento.DatosCliente::nombre).orElse("—");
        return Result.ok(new DetalleAlerta(resumen(a), a.getTransaccionId(), operaciones, a.getComentario(), revisor,
                a.getFechaRevision(), contextos.billeteraBloqueada(a.getBilleteraId())));
    }

    @Override
    public Result<Void, ErrorCumplimiento> revisar(Long alertaId, UsuarioId administrador, EstadoAlerta decision,
                                                   String comentario, boolean bloquear, String motivoBloqueo) {
        if (bloquear && decision != EstadoAlerta.ESCALADA) return Result.err(new ErrorCumplimiento.BloqueoSoloAlEscalar());
        var motivo = motivoBloqueo == null ? "" : motivoBloqueo.strip();
        if (bloquear && (motivo.length() < 5 || motivo.length() > 150)) {
            return Result.err(new ErrorCumplimiento.MotivoBloqueoInvalido());
        }
        try {
            return ejecutor.ejecutarResultado(() -> {
                var alerta = alertas.porId(alertaId);
                if (alerta.isEmpty()) return Result.err(new ErrorCumplimiento.AlertaNoEncontrada());
                var revision = alerta.get().revisar(decision, comentario, administrador, OffsetDateTime.now(clock));
                if (revision instanceof Result.Err<Void, ErrorCumplimiento> err) return err;
                if (!alertas.guardarRevision(alerta.get())) return Result.err(new ErrorCumplimiento.AlertaYaRevisada());
                if (bloquear) {
                    var fallo = contextos.bloquear(alerta.get().getBilleteraId(), motivo);
                    if (fallo.isPresent()) return Result.err(new ErrorCumplimiento.BloqueoFallido(fallo.get()));
                }
                return Result.ok();
            });
        } catch (org.springframework.dao.OptimisticLockingFailureException e) {
            return Result.err(new ErrorCumplimiento.OperacionConcurrente());
        }
    }

    private ResumenAlerta resumen(Alerta a) {
        var cliente = contextos.titular(a.getBilleteraId()).flatMap(contextos::cliente)
                .map(c -> new Cliente(c.nombre(), c.dniEnmascarado())).orElse(DESCONOCIDO);
        return new ResumenAlerta(a.getId(), a.getRegla(), cliente, a.getDetalle(), a.getFecha(), a.getEstado());
    }
}
