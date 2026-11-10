package com.projects.kapikua_api.credito.infrastructure.adapter.out.externo;

import com.projects.kapikua_api.credito.application.port.out.EntidadFinanciera;
import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.credito.domain.service.CalculadoraFrancesa;
import com.projects.kapikua_api.credito.domain.model.CondicionesCredito;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class EntidadFinancieraSimulada implements EntidadFinanciera {
    private static final Logger log = LoggerFactory.getLogger(EntidadFinancieraSimulada.class);

    @Override
    public Result<RespuestaEfa, ErrorCredito> solicitarDecision(SolicitudEfa s) {
        var codigo = "EFA-S-" + aleatorio();
        if (s.dniCliente() != null && s.dniCliente().endsWith("3")) {
            return Result.ok(new RespuestaEfa.Denegada(codigo, "La entidad aliada no aprobó la solicitud por su política interna."));
        }
        return Result.ok(new RespuestaEfa.Confirmada(codigo,
                new CondicionesCredito(s.monto(), s.cuotas(), s.tea(), CalculadoraFrancesa.tcea(s.tea()), s.cuota())));
    }

    @Override
    public Result<String, ErrorCredito> confirmarDesembolso(String codigoSolicitud) {
        if (codigoSolicitud == null || !codigoSolicitud.startsWith("EFA-S-")) {
            return Result.err(new ErrorCredito.EfaNoDisponible());
        }
        return Result.ok("EFA-C-" + aleatorio());
    }

    @Override
    public Result<Void, ErrorCredito> registrarPago(String codigoCredito, Dinero monto) {
        log.info("Entidad aliada simulada: pago de {} registrado en el crédito {}", monto, codigoCredito);
        return Result.ok();
    }

    private static String aleatorio() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
