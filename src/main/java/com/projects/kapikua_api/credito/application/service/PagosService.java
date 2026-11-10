package com.projects.kapikua_api.credito.application.service;

import com.projects.kapikua_api.credito.application.port.in.PagarPrestamoUseCase;
import com.projects.kapikua_api.credito.application.port.out.OperacionesBilletera;
import com.projects.kapikua_api.credito.application.port.out.OperacionesBilletera.Cargo;
import com.projects.kapikua_api.credito.application.port.out.PrestamoRepositoryPort;
import com.projects.kapikua_api.credito.application.port.out.ReglasCredito;
import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.credito.domain.model.Cuota;
import com.projects.kapikua_api.credito.domain.model.EstadoCuota;
import com.projects.kapikua_api.credito.domain.model.ModalidadPago;
import com.projects.kapikua_api.credito.domain.model.Origen;
import com.projects.kapikua_api.credito.domain.model.Prestamo;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.puerto.EjecutorTransaccional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PagosService implements PagarPrestamoUseCase {
    private final PrestamoRepositoryPort prestamos;
    private final OperacionesBilletera billetera;
    private final SoporteOfertas soporte;
    private final CierreCobranza cierre;
    private final EjecutorTransaccional ejecutor;
    private final Clock clock;

    PagosService(PrestamoRepositoryPort prestamos, OperacionesBilletera billetera, SoporteOfertas soporte,
                 CierreCobranza cierre, EjecutorTransaccional ejecutor, Clock clock) {
        this.prestamos = prestamos;
        this.billetera = billetera;
        this.soporte = soporte;
        this.cierre = cierre;
        this.ejecutor = ejecutor;
        this.clock = clock;
    }

    @Override
    public Result<VistaPago, ErrorCredito> prepararPagoCuotas(UsuarioId usuarioId) {
        var preparado = paraPagar(usuarioId);
        if (preparado instanceof Result.Err<APagar, ErrorCredito> err) return Result.err(err.error());
        var a = ((Result.Ok<APagar, ErrorCredito>) preparado).valor();
        var moratorio = a.cuotas().stream().map(Cuota::getInteresMoratorio).reduce(Dinero.CERO, Dinero::sumar);
        return Result.ok(new VistaPago(a.cuotas().stream().map(Cuota::getNumero).toList(), a.total(), moratorio,
                a.deApartado(), UUID.randomUUID().toString()));
    }

    @Override
    public Result<Dinero, ErrorCredito> pagarCuotas(UsuarioId usuarioId, String referencia) {
        return ejecutor.ejecutarResultado(() -> {
            var preparado = paraPagar(usuarioId);
            if (preparado instanceof Result.Err<APagar, ErrorCredito> err) return Result.<Dinero, ErrorCredito>err(err.error());
            var a = ((Result.Ok<APagar, ErrorCredito>) preparado).valor();
            var p = a.prestamo();
            var cargo = billetera.cobrar(usuarioId, a.total(), a.deApartado(), true, false, referencia,
                    "Pago de " + (a.cuotas().size() == 1 ? "la cuota " : "las cuotas ")
                            + a.cuotas().stream().map(c -> String.valueOf(c.getNumero())).reduce((x, y) -> x + ", " + y).orElse("")
                            + " del crédito " + p.getCodigoCreditoEfa());
            if (cargo instanceof Result.Err<Cargo, ErrorCredito>) {
                return Result.<Dinero, ErrorCredito>err(new ErrorCredito.SaldoInsuficienteParaPagar(a.total().toString()));
            }
            var c = ((Result.Ok<Cargo, ErrorCredito>) cargo).valor();
            if (!c.nuevo()) return Result.<Dinero, ErrorCredito>ok(c.cobrado());
            var ahora = soporte.ahora();
            var sobrante = p.aplicarPago(a.cuotas(), c.cobrado(), true, c.transaccionId(), Origen.DEUDOR, false,
                    LocalDate.now(clock), ahora);
            cierre.cerrar(p, sobrante);
            return Result.<Dinero, ErrorCredito>ok(c.cobrado());
        });
    }

    private record APagar(Prestamo prestamo, List<Cuota> cuotas, Dinero total, Dinero deApartado) {}

    private Result<APagar, ErrorCredito> paraPagar(UsuarioId usuarioId) {
        var activo = prestamos.activoDe(usuarioId);
        if (activo.isEmpty()) return Result.err(new ErrorCredito.PrestamoNoEncontrado());
        var p = activo.get();
        var reglas = soporte.reglas();
        if (reglas instanceof Result.Err<ReglasCredito.Reglas, ErrorCredito> err) return Result.err(err.error());
        var hoy = LocalDate.now(clock);
        p.actualizarMora(hoy, ((Result.Ok<ReglasCredito.Reglas, ErrorCredito>) reglas).valor().tasaMoratoriaMaxima());
        var cuotas = p.cuotasVencidas().isEmpty() ? p.cuotaObjetivo().stream().toList() : p.cuotasVencidas();
        if (cuotas.isEmpty()) return Result.err(new ErrorCredito.NadaQuePagar());
        var total = cuotas.stream().map(Cuota::pendiente).reduce(Dinero.CERO, Dinero::sumar);
        var deApartado = cuotas.stream()
                .map(c -> c.getMontoApartado().menorQue(c.pendiente()) ? c.getMontoApartado() : c.pendiente())
                .reduce(Dinero.CERO, Dinero::sumar);
        return Result.ok(new APagar(p, cuotas, total, deApartado));
    }

    @Override
    public Result<VistaAnticipado, ErrorCredito> prepararAnticipado(UsuarioId usuarioId) {
        var activo = prestamos.activoDe(usuarioId);
        if (activo.isEmpty()) return Result.err(new ErrorCredito.PrestamoNoEncontrado());
        var p = activo.get();
        var pendientes = (int) p.getCuotas().stream().filter(c -> c.getEstado() == EstadoCuota.PENDIENTE).count();
        return Result.ok(new VistaAnticipado(p.capitalPendiente(), pendientes, !p.cuotasVencidas().isEmpty(),
                UUID.randomUUID().toString()));
    }

    @Override
    public Result<Dinero, ErrorCredito> pagarAnticipado(UsuarioId usuarioId, ModalidadPago modalidad, Dinero monto,
                                                        String referencia) {
        return ejecutor.ejecutarResultado(() -> {
            var activo = prestamos.activoDe(usuarioId);
            if (activo.isEmpty()) return Result.<Dinero, ErrorCredito>err(new ErrorCredito.PrestamoNoEncontrado());
            var p = activo.get();
            var aPagar = modalidad == ModalidadPago.TOTAL ? p.montoAnticipadoTotal() : monto;
            var verificacion = p.verificarAnticipado(aPagar, modalidad);
            if (verificacion instanceof Result.Err<Void, ErrorCredito> err) return Result.<Dinero, ErrorCredito>err(err.error());

            var usarApartado = modalidad == ModalidadPago.TOTAL ? p.totalApartado() : Dinero.CERO;
            var cargo = billetera.cobrar(usuarioId, aPagar, usarApartado, true, true, referencia,
                    "Pago anticipado del crédito " + p.getCodigoCreditoEfa());
            if (cargo instanceof Result.Err<Cargo, ErrorCredito>) {
                return Result.<Dinero, ErrorCredito>err(new ErrorCredito.SaldoInsuficienteParaPagar(aPagar.toString()));
            }
            var c = ((Result.Ok<Cargo, ErrorCredito>) cargo).valor();
            if (!c.nuevo()) return Result.<Dinero, ErrorCredito>ok(c.cobrado());
            var ahora = soporte.ahora();
            var sobrante = p.aplicarAnticipado(c.cobrado(), modalidad, c.deApartado(), c.transaccionId(), ahora);
            cierre.cerrar(p, sobrante);
            return Result.<Dinero, ErrorCredito>ok(c.cobrado());
        });
    }

    @Override
    public Result<Dinero, ErrorCredito> revocarCargo(UsuarioId usuarioId) {
        return ejecutor.ejecutarResultado(() -> {
            var activo = prestamos.activoDe(usuarioId);
            if (activo.isEmpty()) return Result.<Dinero, ErrorCredito>err(new ErrorCredito.PrestamoNoEncontrado());
            var p = activo.get();
            var revocado = p.revocarCargo(soporte.ahora());
            if (revocado instanceof Result.Ok<Dinero, ErrorCredito> ok) {
                billetera.liberarApartado(usuarioId, ok.valor());
                prestamos.guardar(p);
            }
            return revocado;
        });
    }
}
