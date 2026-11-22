package com.projects.kapikua_api.cumplimiento.client;

import com.projects.kapikua_api.billetera.service.BilleteraService;
import com.projects.kapikua_api.billetera.service.ConsultaBilleteraService;
import com.projects.kapikua_api.billetera.error.ErrorBilletera;
import com.projects.kapikua_api.cumplimiento.model.OperacionObservada;
import com.projects.kapikua_api.identidad.service.IdentidadService;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Component
class ContextosCumplimientoAdapter implements ContextosCumplimiento {
    private final ConsultaBilleteraService billetera;
    private final BilleteraService estadoBilletera;
    private final IdentidadService identidad;

    ContextosCumplimientoAdapter(ConsultaBilleteraService billetera, BilleteraService estadoBilletera,
                                 IdentidadService identidad) {
        this.billetera = billetera;
        this.estadoBilletera = estadoBilletera;
        this.identidad = identidad;
    }

    @Override
    public List<OperacionObservada> operaciones(Long billeteraId, OffsetDateTime desde, OffsetDateTime hasta) {
        return billetera.operacionesDelTitular(billeteraId, desde, hasta).stream()
                .map(o -> new OperacionObservada(o.transaccionId(), o.tipo(), o.monto(), o.fecha()))
                .toList();
    }

    @Override
    public Optional<UsuarioId> titular(Long billeteraId) {
        return billetera.titularDe(billeteraId);
    }

    @Override
    public boolean billeteraBloqueada(Long billeteraId) {
        return billetera.bloqueada(billeteraId);
    }

    @Override
    public Optional<String> bloquear(Long billeteraId, String motivo) {
        return switch (estadoBilletera.bloquearPorCumplimiento(billeteraId, motivo)) {
            case Result.Ok<Void, ErrorBilletera> ok -> Optional.empty();
            case Result.Err<Void, ErrorBilletera> err -> Optional.of(err.error().mensaje());
        };
    }

    @Override
    public boolean operacionDelUsuario(UsuarioId usuarioId, Long transaccionId) {
        return billetera.comprobante(usuarioId, transaccionId).isPresent();
    }

    @Override
    public Optional<DatosCliente> cliente(UsuarioId usuarioId) {
        return identidad.porId(usuarioId)
                .map(d -> new DatosCliente(d.nombres() + " " + d.apellidos(), d.dniEnmascarado()));
    }
}
