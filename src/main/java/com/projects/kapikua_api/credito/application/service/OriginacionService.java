package com.projects.kapikua_api.credito.application.service;

import com.projects.kapikua_api.credito.application.port.in.SimularCreditoUseCase;
import com.projects.kapikua_api.credito.application.port.in.SolicitarCreditoUseCase;
import com.projects.kapikua_api.credito.application.port.out.ClientesCredito;
import com.projects.kapikua_api.credito.application.port.out.EvaluacionCrediticia;
import com.projects.kapikua_api.credito.application.port.out.OfertaRepositoryPort;
import com.projects.kapikua_api.credito.application.port.out.PrestamoRepositoryPort;
import com.projects.kapikua_api.credito.application.port.out.ReglasCredito;
import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.credito.domain.service.CalculadoraFrancesa;
import com.projects.kapikua_api.credito.domain.model.Oferta;
import com.projects.kapikua_api.evaluacion.application.port.in.ConsultarEvaluacionUseCase.NivelCliente;
import com.projects.kapikua_api.evaluacion.application.port.in.ResultadoEvaluacion;
import com.projects.kapikua_api.evaluacion.domain.model.Destino;
import com.projects.kapikua_api.evaluacion.domain.model.Recomendacion;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.puerto.EjecutorTransaccional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

/** CU-07 Simular crédito y CU-08 Solicitar crédito. */
@Service
public class OriginacionService implements SimularCreditoUseCase, SolicitarCreditoUseCase {

    private final EvaluacionCrediticia evaluacion;
    private final ClientesCredito clientes;
    private final OfertaRepositoryPort ofertas;
    private final PrestamoRepositoryPort prestamos;
    private final SoporteOfertas soporte;
    private final DecisionEntidad decision;
    private final EjecutorTransaccional ejecutor;
    private final Clock clock;

    OriginacionService(EvaluacionCrediticia evaluacion, ClientesCredito clientes, OfertaRepositoryPort ofertas,
                       PrestamoRepositoryPort prestamos, SoporteOfertas soporte, DecisionEntidad decision,
                       EjecutorTransaccional ejecutor, Clock clock) {
        this.evaluacion = evaluacion;
        this.clientes = clientes;
        this.ofertas = ofertas;
        this.prestamos = prestamos;
        this.soporte = soporte;
        this.decision = decision;
        this.ejecutor = ejecutor;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ CU-07

    @Override
    public Result<Opciones, ErrorCredito> opciones(UsuarioId usuarioId) {
        var nivel = evaluacion.nivel(usuarioId);
        if (nivel instanceof Result.Err<NivelCliente, ErrorCredito> err) return Result.err(err.error());
        var reglas = soporte.reglas();
        if (reglas instanceof Result.Err<ReglasCredito.Reglas, ErrorCredito> err) return Result.err(err.error());
        var n = ((Result.Ok<NivelCliente, ErrorCredito>) nivel).valor();
        return Result.ok(new Opciones(n.nivel(), n.cuotasMinimas(), n.maxCuotas(), n.tea(),
                ((Result.Ok<ReglasCredito.Reglas, ErrorCredito>) reglas).valor().topePorOperacion()));
    }

    @Override
    public Result<Simulacion, ErrorCredito> simular(UsuarioId usuarioId, Dinero monto, int cuotas) {
        var resultado = opciones(usuarioId);
        if (resultado instanceof Result.Err<Opciones, ErrorCredito> err) return Result.err(err.error());
        var op = ((Result.Ok<Opciones, ErrorCredito>) resultado).valor();
        if (monto.esCero() || monto.mayorQue(op.montoMaximo())) return Result.err(new ErrorCredito.MontoInvalido());
        if (cuotas < op.cuotasMinimas() || cuotas > op.cuotasMaximas()) {
            return Result.err(new ErrorCredito.CuotasFueraDeRango(op.cuotasMinimas(), op.cuotasMaximas()));
        }
        var cronograma = CalculadoraFrancesa.cronograma(monto, op.tea(), cuotas, LocalDate.now(clock));
        var reglas = ((Result.Ok<ReglasCredito.Reglas, ErrorCredito>) soporte.reglas()).valor();
        if (cronograma.cuota().menorQue(reglas.cuotaMinima())) {
            return Result.err(new ErrorCredito.CuotaMinima(reglas.cuotaMinima().toString()));
        }
        return Result.ok(new Simulacion(op, monto, cuotas, op.tea(), CalculadoraFrancesa.tcea(op.tea()),
                cronograma.cuota(), cronograma.interesTotal(), cronograma.totalAPagar(), cronograma.cuotas()));
    }

    // ------------------------------------------------------------------ CU-08

    @Override
    public Result<ResultadoSolicitud, ErrorCredito> solicitar(UsuarioId usuarioId, Dinero monto, int cuotas,
                                                              Destino destino, boolean autorizaDatos) {
        if (!autorizaDatos) return Result.err(new ErrorCredito.ConsentimientoRequerido());
        // RN-09: un solo préstamo activo.
        if (prestamos.activoDe(usuarioId).isPresent()) return Result.err(new ErrorCredito.PrestamoActivoExistente());
        // Una solicitud a la vez: la anterior debe haber terminado o vencido.
        var anterior = ofertas.ultimaDe(usuarioId);
        if (anterior.isPresent() && anterior.get().getEstado().enCurso()) {
            var alDia = soporte.actualizada(anterior.get().getId(), usuarioId);
            if (alDia instanceof Result.Ok<Oferta, ErrorCredito> ok && ok.valor().getEstado().enCurso()) {
                return Result.err(new ErrorCredito.OfertaEnCurso());
            }
        }

        // CU-08, paso 2: consentimientos; pasos 3 a 6: evaluación (la guarda Evaluación de forma inmutable).
        clientes.otorgarConsentimientosDeSolicitud(usuarioId);
        var evaluada = evaluacion.evaluar(usuarioId, monto, cuotas, destino);
        if (evaluada instanceof Result.Err<ResultadoEvaluacion, ErrorCredito> err) return Result.err(err.error());
        var ev = ((Result.Ok<ResultadoEvaluacion, ErrorCredito>) evaluada).valor();
        if (ev.recomendacion() == Recomendacion.NO_APTO) {
            return Result.ok(new ResultadoSolicitud(null, ev.recomendacion(), ev.motivo(), false));
        }

        var reglas = soporte.reglas();
        if (reglas instanceof Result.Err<ReglasCredito.Reglas, ErrorCredito> err) return Result.err(err.error());
        var vigencia = ((Result.Ok<ReglasCredito.Reglas, ErrorCredito>) reglas).valor().vigenciaOfertaDias();
        var oferta = ejecutor.ejecutar(() -> ofertas.guardar(
                Oferta.crear(ev.evaluacionId(), usuarioId, soporte.ahora(), clock.getZone(), vigencia)));

        // Sin aval (nivel 3 o más), se pide la decisión de inmediato. Si la entidad no responde, queda pendiente.
        if (!ev.requiereAval()) decision.pedirSiEstaLista(oferta.getId());
        return Result.ok(new ResultadoSolicitud(oferta.getId(), ev.recomendacion(), ev.motivo(), ev.requiereAval()));
    }
}
