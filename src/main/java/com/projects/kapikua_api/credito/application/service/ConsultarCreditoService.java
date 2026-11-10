package com.projects.kapikua_api.credito.application.service;

import com.projects.kapikua_api.credito.application.port.in.ConsultarCreditoUseCase;
import com.projects.kapikua_api.credito.application.port.out.ClientesCredito;
import com.projects.kapikua_api.credito.application.port.out.OfertaRepositoryPort;
import com.projects.kapikua_api.credito.application.port.out.PrestamoRepositoryPort;
import com.projects.kapikua_api.credito.application.port.out.ReglasCredito;
import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.credito.domain.model.EstadoAval;
import com.projects.kapikua_api.credito.domain.model.EstadoOferta;
import com.projects.kapikua_api.credito.domain.model.Oferta;
import com.projects.kapikua_api.credito.domain.model.Prestamo;
import com.projects.kapikua_api.evaluacion.application.port.in.ResultadoEvaluacion;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

/** CU-12 y el panel de crédito: préstamo vigente, solicitud en curso e invitaciones de aval. */
@Service
public class ConsultarCreditoService implements ConsultarCreditoUseCase {

    private final SoporteOfertas soporte;
    private final OfertaRepositoryPort ofertas;
    private final PrestamoRepositoryPort prestamos;
    private final ClientesCredito clientes;
    private final Clock clock;

    ConsultarCreditoService(SoporteOfertas soporte, OfertaRepositoryPort ofertas, PrestamoRepositoryPort prestamos,
                            ClientesCredito clientes, Clock clock) {
        this.soporte = soporte;
        this.ofertas = ofertas;
        this.prestamos = prestamos;
        this.clientes = clientes;
        this.clock = clock;
    }

    @Override
    public Panel panel(UsuarioId usuarioId) {
        var prestamo = prestamos.activoDe(usuarioId).map(this::detalle).orElse(null);
        DetalleOferta oferta = null;
        var ultima = ofertas.ultimaDe(usuarioId);
        if (ultima.isPresent() && ultima.get().getEstado() != EstadoOferta.ACEPTADA) {
            var r = oferta(usuarioId, ultima.get().getId());
            if (r instanceof Result.Ok<DetalleOferta, ErrorCredito> ok) oferta = ok.valor();
        }
        var plazo = plazoAval();
        var invitaciones = ofertas.conInvitacionPendientePara(usuarioId).stream()
                .map(o -> soporte.actualizada(o.getId(), usuarioId))
                .filter(r -> r instanceof Result.Ok)
                .map(r -> ((Result.Ok<Oferta, ErrorCredito>) r).valor())
                .flatMap(o -> o.invitacionPendientePara(usuarioId).stream().map(a -> new Invitacion(o.getId(),
                        clientes.nombreEnmascarado(o.getUsuarioId()), montoSugerido(o),
                        a.venceEn(plazo, o.finVigencia(clock.getZone())))))
                .toList();
        var garantias = prestamos.idsQueRespalda(usuarioId).stream()
                .map(prestamos::porId).flatMap(Optional::stream)
                .map(p -> {
                    var aval = ofertas.porId(p.getOfertaId()).flatMap(Oferta::avalAceptadoOActivo).orElseThrow();
                    return new Garantia(p.getId(), clientes.nombreEnmascarado(p.getUsuarioId()), aval.getEstado(), p.getEstado(),
                            p.getMonto(), p.cuotasVencidas().size(), p.totalVencido(), aval.getFechaAviso());
                })
                .toList();
        return new Panel(prestamo, oferta, invitaciones, garantias);
    }

    @Override
    public Result<DetalleOferta, ErrorCredito> oferta(UsuarioId usuarioId, Long ofertaId) {
        var cargada = soporte.actualizada(ofertaId, usuarioId);
        if (cargada instanceof Result.Err<Oferta, ErrorCredito> err) return Result.err(err.error());
        var o = ((Result.Ok<Oferta, ErrorCredito>) cargada).valor();
        if (!o.getUsuarioId().equals(usuarioId)) return Result.err(new ErrorCredito.OfertaNoEncontrada());
        var ev = soporte.evaluacionDe(o);
        if (ev instanceof Result.Err<ResultadoEvaluacion, ErrorCredito> err) return Result.err(err.error());
        var e = ((Result.Ok<ResultadoEvaluacion, ErrorCredito>) ev).valor();

        var plazo = plazoAval();
        var avales = o.getAvales().stream().map(a -> new AvalVista(clientes.nombreEnmascarado(a.getUsuarioAvalId()),
                a.getEstado(), a.getEstado() == EstadoAval.PENDIENTE ? a.venceEn(plazo, o.finVigencia(clock.getZone())) : null))
                .toList();
        var confirmada = o.getCondiciones() != null;
        var c = e.condiciones();
        var enCurso = o.getEstado() == EstadoOferta.PENDIENTE_EFA;
        return Result.ok(new DetalleOferta(o.getId(), o.getEstado(), e.recomendacion(), e.motivo(),
                confirmada ? o.getCondiciones().monto() : c.monto(),
                confirmada ? o.getCondiciones().cuotas() : c.cuotas(),
                confirmada ? o.getCondiciones().tea() : c.tea(),
                confirmada ? o.getCondiciones().cuota() : c.cuota(),
                confirmada, e.requiereAval(), avales, o.getVigenteHasta(), o.getMotivoDenegacion(),
                enCurso && e.requiereAval() && o.avalAceptado().isEmpty()
                        && o.getAvales().stream().noneMatch(a -> a.getEstado().enCurso()),
                o.listaParaEfa(e.requiereAval()),
                o.getEstado() == EstadoOferta.CONFIRMADA,
                o.getCondiciones()));
    }

    private DetallePrestamo detalle(Prestamo p) {
        var aval = ofertas.porId(p.getOfertaId()).flatMap(Oferta::avalAceptadoOActivo)
                .map(a -> clientes.nombreEnmascarado(a.getUsuarioAvalId()) + " (" + a.getEstado().name().toLowerCase() + ")")
                .orElse(null);
        return new DetallePrestamo(p.getId(), p.getEstado(), p.getCodigoCreditoEfa(), p.getMonto(), p.getTea(),
                p.getTcea(), p.getPorcentajeRetencion(), p.capitalPendiente(), p.getConstancia(), aval, p.getCuotas(),
                p.isCargoAutorizado(), p.totalApartado(), p.cuotasVencidas().size(), p.totalVencido());
    }

    private Dinero montoSugerido(Oferta o) {
        return soporte.evaluacionDe(o) instanceof Result.Ok<ResultadoEvaluacion, ErrorCredito> ok
                ? ok.valor().condiciones().monto() : Dinero.CERO;
    }

    private Duration plazoAval() {
        return soporte.reglas() instanceof Result.Ok<ReglasCredito.Reglas, ErrorCredito> ok
                ? ok.valor().plazoRespuestaAval() : Duration.ofHours(48);
    }
}
