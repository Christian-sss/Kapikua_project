package com.projects.kapikua_api.evaluacion.infrastructure.adapter.out.contexto;

import com.projects.kapikua_api.billetera.application.port.in.ConsultarBilleteraUseCase;
import com.projects.kapikua_api.evaluacion.application.port.out.CobrosCliente;
import com.projects.kapikua_api.evaluacion.application.port.out.IdentidadCliente;
import com.projects.kapikua_api.evaluacion.domain.model.CobrosDelPeriodo;
import com.projects.kapikua_api.identidad.application.port.in.ConsultarIdentidadUseCase;
import com.projects.kapikua_api.identidad.domain.model.TipoConsentimiento;
import com.projects.kapikua_api.shared.UsuarioId;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
class CobrosClienteAdapter implements CobrosCliente {
    private final ConsultarBilleteraUseCase billetera;

    CobrosClienteAdapter(ConsultarBilleteraUseCase billetera) {
        this.billetera = billetera;
    }

    @Override
    public Optional<CobrosDelPeriodo> delPeriodo(UsuarioId usuarioId, LocalDate fechaSolicitud, int semanas) {
        return billetera.cobros(usuarioId, fechaSolicitud, semanas).map(r -> new CobrosDelPeriodo(r.semanas(),
                r.totalCobrado(), r.semanasConCobros(), r.cantidadCobros(), r.pagadoresDistintos(), r.diasAntiguedad()));
    }
}

@Component
class IdentidadClienteAdapter implements IdentidadCliente {
    private final ConsultarIdentidadUseCase identidad;

    IdentidadClienteAdapter(ConsultarIdentidadUseCase identidad) {
        this.identidad = identidad;
    }

    @Override
    public Optional<DatosCliente> datos(UsuarioId usuarioId) {
        return identidad.porId(usuarioId).map(d -> new DatosCliente(d.dni(), d.identidadValidada()));
    }

    @Override
    public boolean autorizoEvaluacionConBilletera(UsuarioId usuarioId) {
        return identidad.consentimientoVigente(usuarioId, TipoConsentimiento.EVALUACION_CON_BILLETERA);
    }
}
