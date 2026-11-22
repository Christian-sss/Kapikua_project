package com.projects.kapikua_api.credito.service;

import com.projects.kapikua_api.credito.client.ClientesCredito;
import com.projects.kapikua_api.credito.client.EntidadFinanciera;
import com.projects.kapikua_api.credito.repository.OfertaRepository;
import com.projects.kapikua_api.credito.client.OperacionesBilletera;
import com.projects.kapikua_api.credito.repository.PrestamoRepository;
import com.projects.kapikua_api.credito.model.ReglasCredito;
import com.projects.kapikua_api.credito.error.ErrorCredito;
import com.projects.kapikua_api.credito.dto.HojaResumen;
import com.projects.kapikua_api.credito.model.CambioEstado;
import com.projects.kapikua_api.credito.model.ConstanciaAceptacion;
import com.projects.kapikua_api.credito.model.Oferta;
import com.projects.kapikua_api.credito.model.Prestamo;
import com.projects.kapikua_api.credito.model.CalculadoraFrancesa;
import com.projects.kapikua_api.evaluacion.application.port.in.ResultadoEvaluacion;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.puerto.EjecutorTransaccional;
import com.projects.kapikua_api.shared.puerto.CodigoUnSoloUso;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

@Service
public class AceptacionService {
    private static final Logger log = LoggerFactory.getLogger(AceptacionService.class);

    private final SoporteOfertas soporte;
    private final DecisionEntidad decision;
    private final OfertaRepository ofertas;
    private final PrestamoRepository prestamos;
    private final EntidadFinanciera entidad;
    private final OperacionesBilletera billetera;
    private final ClientesCredito clientes;
    private final CodigoUnSoloUso codigos;
    private final PublicadorCredito publicador;
    private final EjecutorTransaccional ejecutor;
    private final Clock clock;
    private final String versionHojaResumen;

    AceptacionService(SoporteOfertas soporte, DecisionEntidad decision, OfertaRepository ofertas,
                      PrestamoRepository prestamos, EntidadFinanciera entidad, OperacionesBilletera billetera,
                      ClientesCredito clientes, CodigoUnSoloUso codigos, PublicadorCredito publicador,
                      EjecutorTransaccional ejecutor, Clock clock,
                      @Value("${kapikua.credito.version-hoja-resumen:HR-1.0}") String versionHojaResumen) {
        this.soporte = soporte;
        this.decision = decision;
        this.ofertas = ofertas;
        this.prestamos = prestamos;
        this.entidad = entidad;
        this.billetera = billetera;
        this.clientes = clientes;
        this.codigos = codigos;
        this.publicador = publicador;
        this.ejecutor = ejecutor;
        this.clock = clock;
        this.versionHojaResumen = versionHojaResumen;
    }

    public Result<Void, ErrorCredito> enviarAEntidad(
            UsuarioId usuarioId,
            Long ofertaId
    ) {
        var cargada = soporte.actualizada(ofertaId, usuarioId);

        if (cargada instanceof Result.Err<Oferta, ErrorCredito> err)
            return Result.err(err.error());

        if (!((Result.Ok<Oferta, ErrorCredito>) cargada).valor().getUsuarioId().equals(usuarioId)) {
            return Result.err(new ErrorCredito.OfertaNoEncontrada());
        }

        return decision.pedirSiEstaLista(ofertaId);
    }

    public Result<HojaResumen, ErrorCredito> hojaResumen(UsuarioId usuarioId, Long ofertaId) {
        var preparada = aceptable(usuarioId, ofertaId);
        if (preparada instanceof Result.Err<Preparada, ErrorCredito> err) return Result.err(err.error());
        var p = ((Result.Ok<Preparada, ErrorCredito>) preparada).valor();
        var c = p.oferta().getCondiciones();
        var cronograma = CalculadoraFrancesa.cronograma(c.monto(), c.tea(), c.cuotas(), LocalDate.now(clock));
        var codigo = codigos.generar(usuarioId, CodigoUnSoloUso.Proposito.ACEPTAR_OFERTA, detalle(ofertaId));
        var aval = p.oferta().avalAceptado().map(a -> clientes.nombreEnmascarado(a.getUsuarioAvalId())).orElse(null);
        return Result.ok(new HojaResumen(ofertaId, versionHojaResumen, c, cronograma.interesTotal(),
                cronograma.totalAPagar(), p.reglas().tasaMoratoriaMaxima(), p.evaluacion().condiciones().porcentajeRetencion(),
                aval, p.oferta().getVigenteHasta(), cronograma.cuotas(), codigo));
    }

    public Result<Long, ErrorCredito> aceptar(UsuarioId usuarioId, Long ofertaId, String codigo, String dispositivo,
                                              boolean autorizaCargo) {
        if (!autorizaCargo) return Result.err(new ErrorCredito.CargoNoAutorizado());
        var preparada = aceptable(usuarioId, ofertaId);
        if (preparada instanceof Result.Err<Preparada, ErrorCredito> err) return Result.err(err.error());
        var p = ((Result.Ok<Preparada, ErrorCredito>) preparada).valor();
        if (prestamos.activoDe(usuarioId).isPresent()) return Result.err(new ErrorCredito.PrestamoActivoExistente());

        var valido = codigos.validar(usuarioId, CodigoUnSoloUso.Proposito.ACEPTAR_OFERTA, detalle(ofertaId), codigo);
        if (valido instanceof Result.Err<Void, CodigoUnSoloUso.CodigoError> err) {
            return Result.err(new ErrorCredito.CodigoNoValido(err.error().mensaje()));
        }
        var monto = p.oferta().getCondiciones().monto();
        var ingreso = billetera.verificarIngreso(usuarioId, monto);
        if (ingreso instanceof Result.Err<Void, ErrorCredito> err) return Result.err(err.error());

        var desembolso = entidad.confirmarDesembolso(p.oferta().getCodigoSolicitudEfa());
        if (desembolso instanceof Result.Err<String, ErrorCredito> err) return Result.err(err.error());
        var codigoCredito = ((Result.Ok<String, ErrorCredito>) desembolso).valor();

        var porcentaje = p.evaluacion().condiciones().porcentajeRetencion();
        var resultado = ejecutor.ejecutarResultado(() -> {
            var o = ofertas.porId(ofertaId).orElseThrow();
            var ahora = soporte.ahora();
            var aceptada = o.aceptar(p.evaluacion().requiereAval(), ahora, clock.getZone());
            if (aceptada instanceof Result.Err<CambioEstado, ErrorCredito> err) {
                return Result.<Long, ErrorCredito>err(err.error());
            }
            var guardada = ofertas.guardar(o);
            var nuevo = Prestamo.desembolsar(guardada, codigoCredito, porcentaje,
                    new ConstanciaAceptacion(ahora, dispositivo, versionHojaResumen), ahora, clock.getZone());
            var prestamo = prestamos.registrar(nuevo);
            var abono = billetera.acreditarDesembolso(usuarioId, monto, "DES-" + codigoCredito,
                    "Desembolso del crédito " + codigoCredito);
            if (abono instanceof Result.Err<Long, ErrorCredito> err) return Result.<Long, ErrorCredito>err(err.error());
            publicador.desembolsado(prestamo);
            return Result.<Long, ErrorCredito>ok(prestamo.getId());
        });
        if (resultado instanceof Result.Err<Long, ErrorCredito> err) {
            log.error("La entidad aliada desembolsó el crédito {} pero no se pudo registrar ({}): requiere reversión",
                    codigoCredito, err.error().codigo());
        }
        return resultado;
    }

    private record Preparada(
            Oferta oferta,
            ResultadoEvaluacion evaluacion,
            ReglasCredito reglas
    ) {
    }

    private Result<Preparada, ErrorCredito> aceptable(UsuarioId usuarioId, Long ofertaId) {

        var cargada = soporte.actualizada(ofertaId, usuarioId);

        if (cargada instanceof Result.Err<Oferta, ErrorCredito> err)
            return Result.err(err.error());

        var oferta = ((Result.Ok<Oferta, ErrorCredito>) cargada).valor();

        if (!oferta.getUsuarioId().equals(usuarioId))
            return Result.err(new ErrorCredito.OfertaNoEncontrada());

        var ev = soporte.evaluacionDe(oferta);

        if (ev instanceof Result.Err<ResultadoEvaluacion, ErrorCredito> err)
            return Result.err(err.error());

        var evaluacion = ((Result.Ok<ResultadoEvaluacion, ErrorCredito>) ev).valor();

        var reglas = soporte.reglas();

        if (reglas instanceof Result.Err<ReglasCredito, ErrorCredito> err)
            return Result.err(err.error());

        var verificacion = oferta.verificarAceptable(evaluacion.requiereAval(), soporte.ahora(), clock.getZone());

        if (verificacion instanceof Result.Err<Void, ErrorCredito> err)
            return Result.err(err.error());

        return Result.ok(new Preparada(oferta, evaluacion, ((Result.Ok<ReglasCredito, ErrorCredito>) reglas).valor()));
    }

    private static String detalle(Long ofertaId) {
        return "oferta|" + ofertaId;
    }
}
