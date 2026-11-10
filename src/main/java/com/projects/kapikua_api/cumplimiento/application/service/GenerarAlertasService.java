package com.projects.kapikua_api.cumplimiento.application.service;

import com.projects.kapikua_api.cumplimiento.application.port.in.GenerarAlertasUseCase;
import com.projects.kapikua_api.cumplimiento.application.port.out.AlertaRepositoryPort;
import com.projects.kapikua_api.cumplimiento.application.port.out.ContextosCumplimiento;
import com.projects.kapikua_api.cumplimiento.domain.model.Alerta;
import com.projects.kapikua_api.cumplimiento.domain.model.OperacionObservada;
import com.projects.kapikua_api.cumplimiento.domain.service.ReglasOperacionesInusuales;
import com.projects.kapikua_api.shared.evento.TransaccionRegistrada;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Set;

/**
 * RN-28. Solo cuentan las operaciones que decide el titular: recargas (entran a su billetera) y envíos
 * (transferencias y pagos a negocio que salen de ella). Desembolsos y cobros de cuotas no generan alertas.
 */
@Service
public class GenerarAlertasService implements GenerarAlertasUseCase {

    private static final Set<String> TIPOS_OBSERVADOS = Set.of("RECARGA", "TRANSFERENCIA", "PAGO_NEGOCIO");

    private final AlertaRepositoryPort alertas;
    private final ContextosCumplimiento contextos;
    private final Clock clock;

    public GenerarAlertasService(AlertaRepositoryPort alertas, ContextosCumplimiento contextos, Clock clock) {
        this.alertas = alertas;
        this.contextos = contextos;
        this.clock = clock;
    }

    /** Se llama después del commit de la operación: corre en su propia transacción. */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int evaluar(TransaccionRegistrada operacion) {
        if (!TIPOS_OBSERVADOS.contains(operacion.tipo())) return 0;
        var billeteraId = "RECARGA".equals(operacion.tipo()) ? operacion.billeteraDestinoId() : operacion.billeteraOrigenId();
        if (billeteraId == null) return 0;

        var actual = new OperacionObservada(operacion.transaccionId(), operacion.tipo(), operacion.monto(), operacion.fecha());
        var ultimas = new ArrayList<OperacionObservada>();
        for (var o : contextos.operaciones(billeteraId,
                operacion.fecha().minus(ReglasOperacionesInusuales.VENTANA_FRACCIONAMIENTO), operacion.fecha().plusSeconds(1))) {
            if (!o.transaccionId().equals(actual.transaccionId())) ultimas.add(o);
        }
        ultimas.add(actual);   // con la fecha exacta del evento (la base guarda microsegundos)

        var tope = contextos.topePorOperacion(operacion.fecha().atZoneSameInstant(clock.getZone()).toLocalDate());
        var detectadas = ReglasOperacionesInusuales.evaluar(actual, ultimas, tope, alertas.ultimasPorRegla(billeteraId));
        int nuevas = 0;
        for (var d : detectadas) {
            if (alertas.registrar(Alerta.nueva(billeteraId, actual.transaccionId(), d.regla(), d.detalle(), operacion.fecha()))) {
                nuevas++;
            }
        }
        return nuevas;
    }
}
