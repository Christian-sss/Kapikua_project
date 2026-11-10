package com.projects.kapikua_api.cumplimiento.infrastructure.adapter.out.contexto;

import com.projects.kapikua_api.billetera.application.port.in.BloquearBilleteraUseCase;
import com.projects.kapikua_api.billetera.application.port.in.ConsultarBilleteraUseCase;
import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.cumplimiento.application.port.in.ConsultarParametrosUseCase;
import com.projects.kapikua_api.cumplimiento.application.port.out.ContextosCumplimiento;
import com.projects.kapikua_api.cumplimiento.domain.error.ParametroError;
import com.projects.kapikua_api.cumplimiento.domain.model.OperacionObservada;
import com.projects.kapikua_api.identidad.application.port.in.ConsultarIdentidadUseCase;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** Cumplimiento consulta Billetera e Identidad solo por sus puertos de entrada. */
@Component
class ContextosCumplimientoAdapter implements ContextosCumplimiento {

    private final ConsultarBilleteraUseCase billetera;
    private final BloquearBilleteraUseCase bloqueo;
    private final ConsultarIdentidadUseCase identidad;
    private final ConsultarParametrosUseCase parametros;

    ContextosCumplimientoAdapter(ConsultarBilleteraUseCase billetera, BloquearBilleteraUseCase bloqueo,
                                 ConsultarIdentidadUseCase identidad, ConsultarParametrosUseCase parametros) {
        this.billetera = billetera;
        this.bloqueo = bloqueo;
        this.identidad = identidad;
        this.parametros = parametros;
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
        return switch (bloqueo.bloquearPorCumplimiento(billeteraId, motivo)) {
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

    @Override
    public Dinero topePorOperacion(LocalDate fecha) {
        return Dinero.de(valor("UIT", fecha).multiply(valor("TOPE_OPERACION_UIT", fecha)));
    }

    private BigDecimal valor(String codigo, LocalDate fecha) {
        return switch (parametros.valorVigente(codigo, fecha)) {
            case Result.Ok<BigDecimal, ParametroError> ok -> ok.valor();
            case Result.Err<BigDecimal, ParametroError> err -> throw new IllegalStateException(err.error().mensaje());
        };
    }
}
