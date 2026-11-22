package com.projects.kapikua_api.credito.client;

import com.projects.kapikua_api.billetera.service.ConsultaBilleteraService;
import com.projects.kapikua_api.billetera.service.OperacionesCreditoService;
import com.projects.kapikua_api.billetera.error.ErrorBilletera;
import com.projects.kapikua_api.credito.client.ClientesCredito;
import com.projects.kapikua_api.credito.client.EvaluacionCrediticia;
import com.projects.kapikua_api.credito.client.OperacionesBilletera;
import com.projects.kapikua_api.credito.error.ErrorCredito;
import com.projects.kapikua_api.evaluacion.application.port.in.ConsultarEvaluacionUseCase;
import com.projects.kapikua_api.evaluacion.application.port.in.ConsultarEvaluacionUseCase.NivelCliente;
import com.projects.kapikua_api.evaluacion.application.port.in.EvaluarSolicitudUseCase;
import com.projects.kapikua_api.evaluacion.application.port.in.ResultadoEvaluacion;
import com.projects.kapikua_api.evaluacion.domain.error.ErrorEvaluacion;
import com.projects.kapikua_api.evaluacion.domain.model.Destino;
import com.projects.kapikua_api.evaluacion.domain.model.SolicitudCredito;
import com.projects.kapikua_api.identidad.dto.DatosIdentidad;
import com.projects.kapikua_api.identidad.service.IdentidadService;
import com.projects.kapikua_api.identidad.service.ConsentimientoService;
import com.projects.kapikua_api.identidad.entity.TipoConsentimiento;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class EvaluacionCrediticiaAdapter implements EvaluacionCrediticia {
    private final EvaluarSolicitudUseCase evaluar;
    private final ConsultarEvaluacionUseCase consultar;

    EvaluacionCrediticiaAdapter(EvaluarSolicitudUseCase evaluar, ConsultarEvaluacionUseCase consultar) {
        this.evaluar = evaluar;
        this.consultar = consultar;
    }

    @Override
    public Result<ResultadoEvaluacion, ErrorCredito> evaluar(UsuarioId usuarioId, Dinero monto, int cuotas, Destino destino) {
        return switch (evaluar.evaluar(usuarioId, new SolicitudCredito(monto, cuotas, destino))) {
            case Result.Ok<ResultadoEvaluacion, ErrorEvaluacion> ok -> Result.ok(ok.valor());
            case Result.Err<ResultadoEvaluacion, ErrorEvaluacion> err -> Result.err(new ErrorCredito.NoSePudoEvaluar(err.error().mensaje()));
        };
    }

    @Override
    public Result<Void, ErrorCredito> evaluarAval(UsuarioId avalId) {
        return evaluar.evaluarAval(avalId).esOk() ? Result.ok() : Result.err(new ErrorCredito.AvalNoCalifica());
    }

    @Override
    public Optional<ResultadoEvaluacion> evaluacion(Long evaluacionId) {
        return consultar.porId(evaluacionId);
    }

    @Override
    public Result<NivelCliente, ErrorCredito> nivel(UsuarioId usuarioId) {
        return switch (consultar.nivel(usuarioId)) {
            case Result.Ok<NivelCliente, ErrorEvaluacion> ok -> Result.ok(ok.valor());
            case Result.Err<NivelCliente, ErrorEvaluacion> err -> Result.err(new ErrorCredito.NoSePudoEvaluar(err.error().mensaje()));
        };
    }
}

@Component
class OperacionesBilleteraAdapter implements OperacionesBilletera {
    private final OperacionesCreditoService operaciones;
    private final ConsultaBilleteraService consultar;

    OperacionesBilleteraAdapter(OperacionesCreditoService operaciones, ConsultaBilleteraService consultar) {
        this.operaciones = operaciones;
        this.consultar = consultar;
    }

    @Override
    public Result<Void, ErrorCredito> verificarIngreso(UsuarioId usuarioId, Dinero monto) {
        return switch (operaciones.verificarIngreso(usuarioId, monto)) {
            case Result.Ok<Void, ErrorBilletera> ok -> Result.ok();
            case Result.Err<Void, ErrorBilletera> err -> Result.err(new ErrorCredito.DesembolsoNoPermitido(err.error().mensaje()));
        };
    }

    @Override
    public Result<Long, ErrorCredito> acreditarDesembolso(UsuarioId usuarioId, Dinero monto, String referencia,
                                                          String descripcion) {
        return switch (operaciones.acreditarDesembolso(usuarioId, monto, referencia, descripcion)) {
            case Result.Ok<Long, ErrorBilletera> ok -> Result.ok(ok.valor());
            case Result.Err<Long, ErrorBilletera> err -> Result.err(new ErrorCredito.DesembolsoNoPermitido(err.error().mensaje()));
        };
    }

    @Override
    public Optional<UsuarioId> usuarioPorCelular(String celular) {
        return consultar.usuarioPorCelular(celular);
    }

    @Override
    public Dinero apartar(UsuarioId usuarioId, Dinero monto) {
        return operaciones.apartar(usuarioId, monto) instanceof Result.Ok<Dinero, ErrorBilletera> ok ? ok.valor() : Dinero.CERO;
    }

    @Override
    public Result<Void, ErrorCredito> liberarApartado(UsuarioId usuarioId, Dinero monto) {
        return switch (operaciones.liberarApartado(usuarioId, monto)) {
            case Result.Ok<Void, ErrorBilletera> ok -> Result.ok();
            case Result.Err<Void, ErrorBilletera> err ->
                    Result.err(new ErrorCredito.OperacionBilleteraNoPermitida(err.error().mensaje()));
        };
    }

    @Override
    public Result<Cargo, ErrorCredito> cobrar(UsuarioId usuarioId, Dinero monto, Dinero usarApartado, boolean exigirTodo,
                                              boolean anticipado, String referencia, String descripcion) {
        return switch (operaciones.cobrar(usuarioId, monto, usarApartado, exigirTodo, anticipado, referencia, descripcion)) {
            case Result.Ok<com.projects.kapikua_api.billetera.dto.Cargo, ErrorBilletera> ok -> Result.ok(new Cargo(
                    ok.valor().transaccionId(), ok.valor().cobrado(), ok.valor().deApartado(), ok.valor().nuevo()));
            case Result.Err<com.projects.kapikua_api.billetera.dto.Cargo, ErrorBilletera> err -> Result.err(
                    err.error() instanceof ErrorBilletera.SaldoInsuficiente
                            ? new ErrorCredito.SaldoInsuficienteParaPagar(monto.toString())
                            : new ErrorCredito.OperacionBilleteraNoPermitida(err.error().mensaje()));
        };
    }
}

@Component
class ClientesCreditoAdapter implements ClientesCredito {
    private final ConsentimientoService consentimientos;
    private final IdentidadService identidad;

    ClientesCreditoAdapter(ConsentimientoService consentimientos, IdentidadService identidad) {
        this.consentimientos = consentimientos;
        this.identidad = identidad;
    }

    @Override
    public void otorgarConsentimientosDeSolicitud(UsuarioId usuarioId) {
        consentimientos.otorgar(usuarioId, TipoConsentimiento.EVALUACION_CON_BILLETERA);
        consentimientos.otorgar(usuarioId, TipoConsentimiento.ENVIO_DATOS_EFA);
    }

    @Override
    public void otorgarConsentimientoDeGarantia(UsuarioId avalId) {
        consentimientos.otorgar(avalId, TipoConsentimiento.GARANTIA_AVAL);
    }

    @Override
    public Optional<String> dni(UsuarioId usuarioId) {
        return identidad.porId(usuarioId).map(DatosIdentidad::dni);
    }

    @Override
    public String nombreEnmascarado(UsuarioId usuarioId) {
        return identidad.nombreEnmascarado(usuarioId);
    }
}
