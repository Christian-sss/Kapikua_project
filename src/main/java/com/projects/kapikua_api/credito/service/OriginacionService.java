package com.projects.kapikua_api.credito.service;

import com.projects.kapikua_api.credito.client.ClientesCredito;
import com.projects.kapikua_api.credito.client.EvaluacionCrediticia;
import com.projects.kapikua_api.credito.repository.OfertaRepository;
import com.projects.kapikua_api.credito.repository.PrestamoRepository;
import com.projects.kapikua_api.credito.model.ReglasCredito;
import com.projects.kapikua_api.credito.error.ErrorCredito;
import com.projects.kapikua_api.credito.dto.OpcionesCredito;
import com.projects.kapikua_api.credito.dto.ResultadoSolicitud;
import com.projects.kapikua_api.credito.dto.Simulacion;
import com.projects.kapikua_api.credito.model.CalculadoraFrancesa;
import com.projects.kapikua_api.credito.model.Oferta;
import com.projects.kapikua_api.evaluacion.application.port.in.ConsultarEvaluacionUseCase.NivelCliente;
import com.projects.kapikua_api.evaluacion.application.port.in.ResultadoEvaluacion;
import com.projects.kapikua_api.evaluacion.domain.model.Destino;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.puerto.EjecutorTransaccional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

@Service
public class OriginacionService {
    private final EvaluacionCrediticia evaluacion;
    private final ClientesCredito clientes;
    private final OfertaRepository ofertas;
    private final PrestamoRepository prestamos;
    private final SoporteOfertas soporte;
    private final DecisionEntidad decision;
    private final EjecutorTransaccional ejecutor;
    private final Clock clock;

    OriginacionService(EvaluacionCrediticia evaluacion, ClientesCredito clientes, OfertaRepository ofertas,
                       PrestamoRepository prestamos, SoporteOfertas soporte, DecisionEntidad decision,
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

    public Result<OpcionesCredito, ErrorCredito> opciones(UsuarioId usuarioId) {
        var nivel = evaluacion.nivel(usuarioId);
        if (nivel instanceof Result.Err<NivelCliente, ErrorCredito> err) return Result.err(err.error());
        var reglas = soporte.reglas();
        if (reglas instanceof Result.Err<ReglasCredito, ErrorCredito> err) return Result.err(err.error());
        var n = ((Result.Ok<NivelCliente, ErrorCredito>) nivel).valor();
        return Result.ok(new OpcionesCredito(n.nivel(), n.cuotasMinimas(), n.maxCuotas(), n.tea(),
                ((Result.Ok<ReglasCredito, ErrorCredito>) reglas).valor().topePorOperacion()));
    }

    public Result<Simulacion, ErrorCredito> simular(UsuarioId usuarioId, Dinero monto, int cuotas) {
        var resultado = opciones(usuarioId);
        if (resultado instanceof Result.Err<OpcionesCredito, ErrorCredito> err) return Result.err(err.error());
        var op = ((Result.Ok<OpcionesCredito, ErrorCredito>) resultado).valor();
        if (monto.esCero() || monto.mayorQue(op.montoMaximo())) return Result.err(new ErrorCredito.MontoInvalido());
        if (cuotas < op.cuotasMinimas() || cuotas > op.cuotasMaximas()) {
            return Result.err(new ErrorCredito.CuotasFueraDeRango(op.cuotasMinimas(), op.cuotasMaximas()));
        }
        var cronograma = CalculadoraFrancesa.cronograma(monto, op.tea(), cuotas, LocalDate.now(clock));
        var reglas = ((Result.Ok<ReglasCredito, ErrorCredito>) soporte.reglas()).valor();
        if (cronograma.cuota().menorQue(reglas.cuotaMinima())) {
            return Result.err(new ErrorCredito.CuotaMinima(reglas.cuotaMinima().toString()));
        }
        return Result.ok(new Simulacion(op, monto, cuotas, op.tea(), CalculadoraFrancesa.tcea(op.tea()),
                cronograma.cuota(), cronograma.interesTotal(), cronograma.totalAPagar(), cronograma.cuotas()));
    }

    public Result<ResultadoSolicitud, ErrorCredito> solicitar(UsuarioId usuarioId, Dinero monto, int cuotas,
                                                              Destino destino, boolean autorizaDatos) {
        if (!autorizaDatos) return Result.err(new ErrorCredito.ConsentimientoRequerido());
        if (prestamos.activoDe(usuarioId).isPresent()) return Result.err(new ErrorCredito.PrestamoActivoExistente());
        var anterior = ofertas.ultimaDe(usuarioId);
        if (anterior.isPresent() && anterior.get().enCurso()) {
            var alDia = soporte.actualizada(anterior.get().getId(), usuarioId);
            if (alDia instanceof Result.Ok<Oferta, ErrorCredito> ok && ok.valor().enCurso()) {
                return Result.err(new ErrorCredito.OfertaEnCurso());
            }
        }

        clientes.otorgarConsentimientosDeSolicitud(usuarioId);
        var evaluada = evaluacion.evaluar(usuarioId, monto, cuotas, destino);
        if (evaluada instanceof Result.Err<ResultadoEvaluacion, ErrorCredito> err) return Result.err(err.error());
        var ev = ((Result.Ok<ResultadoEvaluacion, ErrorCredito>) evaluada).valor();
        if (!ev.esApta()) {
            return Result.ok(new ResultadoSolicitud(null, ev.recomendacion(), ev.motivo(), false));
        }

        var reglas = soporte.reglas();
        if (reglas instanceof Result.Err<ReglasCredito, ErrorCredito> err) return Result.err(err.error());
        var vigencia = ((Result.Ok<ReglasCredito, ErrorCredito>) reglas).valor().vigenciaOfertaDias();
        var oferta = ejecutor.ejecutar(() -> ofertas.guardar(
                Oferta.crear(ev.evaluacionId(), usuarioId, soporte.ahora(), clock.getZone(), vigencia)));

        if (!ev.requiereAval()) decision.pedirSiEstaLista(oferta.getId());
        return Result.ok(new ResultadoSolicitud(oferta.getId(), ev.recomendacion(), ev.motivo(), ev.requiereAval()));
    }
}
