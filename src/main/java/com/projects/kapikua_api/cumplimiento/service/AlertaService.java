package com.projects.kapikua_api.cumplimiento.service;

import com.projects.kapikua_api.cumplimiento.client.ContextosCumplimiento;
import com.projects.kapikua_api.cumplimiento.dto.Cliente;
import com.projects.kapikua_api.cumplimiento.dto.DetalleAlerta;
import com.projects.kapikua_api.cumplimiento.dto.ResumenAlerta;
import com.projects.kapikua_api.cumplimiento.error.ErrorCumplimiento;
import com.projects.kapikua_api.cumplimiento.model.Alerta;
import com.projects.kapikua_api.cumplimiento.model.EstadoAlerta;
import com.projects.kapikua_api.cumplimiento.model.OperacionObservada;
import com.projects.kapikua_api.cumplimiento.repository.AlertaRepository;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.evento.TransaccionRegistrada;
import com.projects.kapikua_api.shared.puerto.EjecutorTransaccional;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Alertas de operaciones inusuales: se generan al registrar una operación y las revisa el Administrador. */
@Service
public class AlertaService {
    private static final Set<String> TIPOS_OBSERVADOS = Set.of("TRANSFERENCIA", "PAGO_NEGOCIO");
    private static final Cliente DESCONOCIDO = new Cliente("—", "—");

    private final AlertaRepository alertas;
    private final ContextosCumplimiento contextos;
    private final ParametroService parametros;
    private final EjecutorTransaccional ejecutor;
    private final Clock clock;

    public AlertaService(AlertaRepository alertas, ContextosCumplimiento contextos, ParametroService parametros,
                         EjecutorTransaccional ejecutor, Clock clock) {
        this.alertas = alertas;
        this.contextos = contextos;
        this.parametros = parametros;
        this.ejecutor = ejecutor;
        this.clock = clock;
    }

    /** Evalúa las reglas sobre una operación recién registrada. Devuelve cuántas alertas nuevas generó. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int evaluar(TransaccionRegistrada operacion) {
        if (!TIPOS_OBSERVADOS.contains(operacion.tipo())) return 0;
        var billeteraId = operacion.billeteraOrigenId();
        if (billeteraId == null) return 0;

        var actual = new OperacionObservada(operacion.transaccionId(), operacion.tipo(), operacion.monto(), operacion.fecha());
        var ultimas = new ArrayList<OperacionObservada>();
        for (var o : contextos.operaciones(billeteraId,
                operacion.fecha().minus(ReglasOperacionesInusuales.VENTANA_FRACCIONAMIENTO), operacion.fecha().plusSeconds(1))) {
            if (!o.transaccionId().equals(actual.transaccionId())) ultimas.add(o);
        }
        ultimas.add(actual);

        var tope = parametros.topePorOperacion(operacion.fecha().atZoneSameInstant(clock.getZone()).toLocalDate());
        var detectadas = ReglasOperacionesInusuales.evaluar(actual, ultimas, tope, alertas.ultimasPorRegla(billeteraId));
        int nuevas = 0;
        for (var d : detectadas) {
            if (alertas.registrar(Alerta.nueva(billeteraId, actual.transaccionId(), d.regla(), d.detalle(), operacion.fecha()))) {
                nuevas++;
            }
        }
        return nuevas;
    }

    public List<ResumenAlerta> pendientes() {
        return alertas.pendientes().stream().map(this::resumen).toList();
    }

    public List<ResumenAlerta> revisadas(int limite) {
        return alertas.revisadas(limite).stream().map(this::resumen).toList();
    }

    public Result<DetalleAlerta, ErrorCumplimiento> detalle(Long alertaId) {
        var alerta = alertas.porId(alertaId);
        if (alerta.isEmpty()) return Result.err(new ErrorCumplimiento.AlertaNoEncontrada());
        var a = alerta.get();
        var operaciones = contextos.operaciones(a.getBilleteraId(),
                a.getFecha().minus(ReglasOperacionesInusuales.VENTANA_FRACCIONAMIENTO), a.getFecha().plusSeconds(1));
        var revisor = a.getRevisadoPor() == null ? null
                : contextos.cliente(a.getRevisadoPor()).map(ContextosCumplimiento.DatosCliente::nombre).orElse("—");
        return Result.ok(new DetalleAlerta(resumen(a), a.getTransaccionId(), operaciones, a.getComentario(), revisor,
                a.getFechaRevision(), contextos.billeteraBloqueada(a.getBilleteraId())));
    }

    /**
     * La transacción es programática (EjecutorTransaccional) a propósito: un conflicto de versión puede saltar al
     * hacer commit, y así se atrapa aquí y se devuelve como OperacionConcurrente. Con @Transactional saldría del método.
     */
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
        } catch (OptimisticLockingFailureException e) {
            return Result.err(new ErrorCumplimiento.OperacionConcurrente());
        }
    }

    private ResumenAlerta resumen(Alerta a) {
        var cliente = contextos.titular(a.getBilleteraId()).flatMap(contextos::cliente)
                .map(c -> new Cliente(c.nombre(), c.dniEnmascarado())).orElse(DESCONOCIDO);
        return new ResumenAlerta(a.getId(), a.getRegla(), cliente, a.getDetalle(), a.getFecha(), a.getEstado());
    }
}
