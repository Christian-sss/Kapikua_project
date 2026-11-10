package com.projects.kapikua_api.credito.infrastructure.adapter.out.contexto;

import com.projects.kapikua_api.billetera.application.port.in.ConsultarBilleteraUseCase;
import com.projects.kapikua_api.billetera.application.port.in.OperacionesCreditoUseCase;
import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.credito.application.port.out.ClientesCredito;
import com.projects.kapikua_api.credito.application.port.out.EvaluacionCrediticia;
import com.projects.kapikua_api.credito.application.port.out.OperacionesBilletera;
import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.evaluacion.application.port.in.ConsultarEvaluacionUseCase;
import com.projects.kapikua_api.evaluacion.application.port.in.ConsultarEvaluacionUseCase.NivelCliente;
import com.projects.kapikua_api.evaluacion.application.port.in.EvaluarSolicitudUseCase;
import com.projects.kapikua_api.evaluacion.application.port.in.ResultadoEvaluacion;
import com.projects.kapikua_api.evaluacion.domain.error.ErrorEvaluacion;
import com.projects.kapikua_api.evaluacion.domain.model.Destino;
import com.projects.kapikua_api.evaluacion.domain.model.SolicitudCredito;
import com.projects.kapikua_api.identidad.application.port.in.ConsultarIdentidadUseCase;
import com.projects.kapikua_api.identidad.application.port.in.GestionarConsentimientosUseCase;
import com.projects.kapikua_api.identidad.domain.model.TipoConsentimiento;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Evaluación Crediticia: traduce sus errores a los de Créditos. */
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

/** Billetera: desembolso y búsqueda del aval por celular. */
@Component
class OperacionesBilleteraAdapter implements OperacionesBilletera {

    private final OperacionesCreditoUseCase operaciones;
    private final ConsultarBilleteraUseCase consultar;

    OperacionesBilleteraAdapter(OperacionesCreditoUseCase operaciones, ConsultarBilleteraUseCase consultar) {
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
    public void liberarApartado(UsuarioId usuarioId, Dinero monto) {
        operaciones.liberarApartado(usuarioId, monto);
    }

    @Override
    public Result<Cargo, ErrorCredito> cobrar(UsuarioId usuarioId, Dinero monto, Dinero usarApartado, boolean exigirTodo,
                                              boolean anticipado, String referencia, String descripcion) {
        return switch (operaciones.cobrar(usuarioId, monto, usarApartado, exigirTodo, anticipado, referencia, descripcion)) {
            case Result.Ok<OperacionesCreditoUseCase.Cargo, ErrorBilletera> ok -> Result.ok(new Cargo(
                    ok.valor().transaccionId(), ok.valor().cobrado(), ok.valor().deApartado(), ok.valor().nuevo()));
            case Result.Err<OperacionesCreditoUseCase.Cargo, ErrorBilletera> err ->
                    Result.err(new ErrorCredito.DesembolsoNoPermitido(err.error().mensaje()));
        };
    }
}

/** Identidad: consentimientos (RN-07) y datos del cliente. */
@Component
class ClientesCreditoAdapter implements ClientesCredito {

    private final GestionarConsentimientosUseCase consentimientos;
    private final ConsultarIdentidadUseCase identidad;

    ClientesCreditoAdapter(GestionarConsentimientosUseCase consentimientos, ConsultarIdentidadUseCase identidad) {
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
        return identidad.porId(usuarioId).map(ConsultarIdentidadUseCase.DatosIdentidad::dni);
    }

    @Override
    public String nombreEnmascarado(UsuarioId usuarioId) {
        return identidad.porId(usuarioId)
                .map(d -> d.nombres().trim().split("\\s+")[0] + " "
                        + (d.apellidos().isBlank() ? "" : Character.toUpperCase(d.apellidos().trim().charAt(0)) + "."))
                .orElse("Usuario Kapikua");
    }
}
