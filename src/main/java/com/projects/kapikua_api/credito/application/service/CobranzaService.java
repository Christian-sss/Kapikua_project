package com.projects.kapikua_api.credito.application.service;

import com.projects.kapikua_api.credito.application.port.in.CobranzaUseCase;
import com.projects.kapikua_api.credito.application.port.out.OfertaRepositoryPort;
import com.projects.kapikua_api.credito.application.port.out.OperacionesBilletera;
import com.projects.kapikua_api.credito.application.port.out.OperacionesBilletera.Cargo;
import com.projects.kapikua_api.credito.application.port.out.PrestamoRepositoryPort;
import com.projects.kapikua_api.credito.application.port.out.ReglasCredito;
import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.credito.domain.model.Aval;
import com.projects.kapikua_api.credito.domain.model.Oferta;
import com.projects.kapikua_api.credito.domain.model.Origen;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.puerto.EjecutorTransaccional;
import com.projects.kapikua_api.shared.puerto.PublicadorEventos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class CobranzaService implements CobranzaUseCase {
    private static final Logger log = LoggerFactory.getLogger(CobranzaService.class);

    private final PrestamoRepositoryPort prestamos;
    private final OfertaRepositoryPort ofertas;
    private final OperacionesBilletera billetera;
    private final SoporteOfertas soporte;
    private final CierreCobranza cierre;
    private final PublicadorEventos publicador;
    private final EjecutorTransaccional ejecutor;
    private final Clock clock;

    CobranzaService(PrestamoRepositoryPort prestamos, OfertaRepositoryPort ofertas, OperacionesBilletera billetera,
                    SoporteOfertas soporte, CierreCobranza cierre, PublicadorEventos publicador,
                    EjecutorTransaccional ejecutor, Clock clock) {
        this.prestamos = prestamos;
        this.ofertas = ofertas;
        this.billetera = billetera;
        this.soporte = soporte;
        this.cierre = cierre;
        this.publicador = publicador;
        this.ejecutor = ejecutor;
        this.clock = clock;
    }

    @Override
    public void aplicarRetencion(UsuarioId usuarioId, Long transaccionVentaId, Dinero cobro) {
        if (prestamos.ventaYaRetenida(transaccionVentaId)) return;
        var ahora = soporte.ahora();

        var propio = prestamos.activoDe(usuarioId);
        if (propio.isPresent()) {
            var p = propio.get();
            var monto = p.montoARetener(cobro);
            var apartado = monto.esCero() ? Dinero.CERO : billetera.apartar(usuarioId, monto);
            if (!apartado.esCero()) {
                p.registrarRetencion(transaccionVentaId, apartado, ahora);
                prestamos.guardar(p);
                publicador.publicarDe(p);
                return;
            }
        }

        for (var prestamoId : prestamos.idsConAvalActivadoDe(usuarioId)) {
            var p = prestamos.porId(prestamoId).orElseThrow();
            var vencido = p.totalVencido();
            var aval = ofertas.porId(p.getOfertaId()).flatMap(Oferta::avalAceptadoOActivo).filter(Aval::estaActivado);
            if (vencido.esCero() || aval.isEmpty()) continue;

            var porcentaje = Dinero.de(cobro.valor().multiply(BigDecimal.valueOf(aval.get().getPorcentajeRetencionAutorizado()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN));
            var monto = porcentaje.menorQue(vencido) ? porcentaje : vencido;
            if (monto.esCero()) continue;
            var cargo = billetera.cobrar(usuarioId, monto, Dinero.CERO, false, false,
                    "AVAL-" + transaccionVentaId + "-" + prestamoId, "Retención como aval del crédito " + p.getCodigoCreditoEfa());
            if (!(cargo instanceof Result.Ok<Cargo, ErrorCredito> ok) || !ok.valor().nuevo() || ok.valor().cobrado().esCero()) continue;

            var primeraVencida = p.cuotasVencidas().getFirst().getId();
            var sobrante = p.aplicarPago(p.cuotasVencidas(), ok.valor().cobrado(), false, ok.valor().transaccionId(),
                    Origen.AVAL, true, LocalDate.now(clock), ahora);
            p.registrarRetencionDeAval(primeraVencida, transaccionVentaId, ok.valor().cobrado(), ahora);
            cierre.cerrar(p, sobrante);
            return;
        }
    }

    private record Conteo(int cuotasCobradas, Dinero cobrado, boolean enMora, boolean avisoAval,
                          boolean avalActivado, boolean pagado) {}

    @Override
    public ResumenCobro ejecutarCobroDiario(LocalDate hoy) {
        var reglasR = soporte.reglas();
        if (!(reglasR instanceof Result.Ok<ReglasCredito.Reglas, ErrorCredito> okReglas)) {
            log.error("Cobro diario cancelado: faltan parámetros de cobranza");
            return new ResumenCobro(0, 0, 0, Dinero.CERO, 0, 0, 0, 0, 1);
        }
        var reglas = okReglas.valor();

        var ofertasActualizadas = 0;
        for (var id : ofertas.idsConVencimientos(hoy, soporte.ahora().minus(reglas.plazoRespuestaAval()))) {
            if (soporte.actualizada(id, null).esOk()) ofertasActualizadas++;
        }

        var revisados = 0; var cuotas = 0; var cobrado = Dinero.CERO;
        var enMora = 0; var avisos = 0; var activados = 0; var pagados = 0; var errores = 0;
        for (var id : prestamos.idsConCobroPendiente(hoy)) {
            try {
                var c = ejecutor.ejecutar(() -> procesar(id, hoy, reglas));
                revisados++;
                cuotas += c.cuotasCobradas();
                cobrado = cobrado.sumar(c.cobrado());
                if (c.enMora()) enMora++;
                if (c.avisoAval()) avisos++;
                if (c.avalActivado()) activados++;
                if (c.pagado()) pagados++;
            } catch (RuntimeException e) {
                errores++;
                log.error("No se pudo procesar el cobro del préstamo {}", id, e);
            }
        }
        var resumen = new ResumenCobro(ofertasActualizadas, revisados, cuotas, cobrado, enMora, avisos, activados,
                pagados, errores);
        log.info("Cobro diario del {}: {}", hoy, resumen);
        return resumen;
    }

    private Conteo procesar(Long prestamoId, LocalDate hoy, ReglasCredito.Reglas reglas) {
        var p = prestamos.porId(prestamoId).orElseThrow();
        if (!p.estaActivo()) return new Conteo(0, Dinero.CERO, false, false, false, false);
        var ahora = soporte.ahora();
        p.actualizarMora(hoy, reglas.tasaMoratoriaMaxima());

        var cobradas = 0;
        var total = Dinero.CERO;
        var sobrante = Dinero.CERO;
        if (p.isCargoAutorizado()) {
            for (var cuota : List.copyOf(p.cuotasPorCobrarAl(hoy))) {
                var pendiente = cuota.pendiente();
                if (pendiente.esCero()) continue;
                var cargo = billetera.cobrar(p.getUsuarioId(), pendiente, cuota.getMontoApartado(), false, false,
                        "COBRO-" + cuota.getId() + "-" + hoy,
                        "Cuota " + cuota.getNumero() + " del crédito " + p.getCodigoCreditoEfa());
                if (!(cargo instanceof Result.Ok<Cargo, ErrorCredito> ok) || !ok.valor().nuevo()
                        || ok.valor().cobrado().esCero()) continue;
                sobrante = sobrante.sumar(p.aplicarPago(List.of(cuota), ok.valor().cobrado(), true,
                        ok.valor().transaccionId(), Origen.DEUDOR, true, hoy, ahora));
                if (cuota.getMontoPagado().equals(cuota.total())) cobradas++;
                total = total.sumar(ok.valor().cobrado());
            }
        }
        var dias = p.revisarAtraso(hoy);

        var escalamiento = Oferta.Escalamiento.NINGUNO;
        if (dias > 0) {
            var oferta = ofertas.porId(p.getOfertaId()).orElseThrow();
            escalamiento = oferta.escalarAval(dias, reglas.diasAvisoAval(), reglas.diasActivacionAval(), ahora);
            if (escalamiento != Oferta.Escalamiento.NINGUNO) ofertas.guardar(oferta);
        }
        var pagado = cierre.cerrar(p, sobrante);
        return new Conteo(cobradas, total, dias > 0, escalamiento == Oferta.Escalamiento.AVISADO,
                escalamiento == Oferta.Escalamiento.ACTIVADO, pagado);
    }
}
