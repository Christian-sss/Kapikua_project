package com.projects.kapikua_api.credito.service;

import com.projects.kapikua_api.credito.client.OperacionesBilletera;
import com.projects.kapikua_api.credito.client.OperacionesBilletera.Cargo;
import com.projects.kapikua_api.credito.repository.PrestamoRepository;
import com.projects.kapikua_api.credito.model.ReglasCredito;
import com.projects.kapikua_api.credito.error.ErrorCredito;
import com.projects.kapikua_api.credito.dto.VistaAnticipado;
import com.projects.kapikua_api.credito.dto.VistaPago;
import com.projects.kapikua_api.credito.model.Cuota;
import com.projects.kapikua_api.credito.model.ModalidadPago;
import com.projects.kapikua_api.credito.model.Origen;
import com.projects.kapikua_api.credito.model.Prestamo;
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
public class PagosService {
    private final PrestamoRepository prestamos;
    private final OperacionesBilletera billetera;
    private final SoporteOfertas soporte;
    private final CierreCobranza cierre;
    private final EjecutorTransaccional ejecutor;
    private final Clock clock;

    PagosService(PrestamoRepository prestamos, OperacionesBilletera billetera, SoporteOfertas soporte,
                 CierreCobranza cierre, EjecutorTransaccional ejecutor, Clock clock) {
        this.prestamos = prestamos;
        this.billetera = billetera;
        this.soporte = soporte;
        this.cierre = cierre;
        this.ejecutor = ejecutor;
        this.clock = clock;
    }

    public Result<VistaPago, ErrorCredito> prepararPagoCuotas(UsuarioId usuarioId) {
        var preparado = paraPagar(usuarioId);
        if (preparado instanceof Result.Err<APagar, ErrorCredito> err) return Result.err(err.error());
        var a = ((Result.Ok<APagar, ErrorCredito>) preparado).valor();
        var moratorio = a.cuotas().stream().map(Cuota::getInteresMoratorio).reduce(Dinero.CERO, Dinero::sumar);
        return Result.ok(new VistaPago(a.cuotas().stream().map(Cuota::getNumero).toList(), a.total(), moratorio,
                a.deApartado(), UUID.randomUUID().toString()));
    }

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
            if (cargo instanceof Result.Err<Cargo, ErrorCredito> err) return Result.<Dinero, ErrorCredito>err(err.error());
            var c = ((Result.Ok<Cargo, ErrorCredito>) cargo).valor();
            if (!c.nuevo()) return Result.<Dinero, ErrorCredito>ok(c.cobrado());
            var ahora = soporte.ahora();
            return p.aplicarPago(a.cuotas(), c.cobrado(), true, c.transaccionId(), Origen.DEUDOR, false,
                            LocalDate.now(clock), ahora)
                    .flatMap(sobrante -> cierre.cerrar(p, sobrante)).map(pagado -> c.cobrado());
        });
    }

    private record APagar(Prestamo prestamo, List<Cuota> cuotas, Dinero total, Dinero deApartado) {}

    private Result<APagar, ErrorCredito> paraPagar(UsuarioId usuarioId) {
        var activo = prestamos.activoDe(usuarioId);
        if (activo.isEmpty()) return Result.err(new ErrorCredito.PrestamoNoEncontrado());
        var p = activo.get();
        var reglas = soporte.reglas();
        if (reglas instanceof Result.Err<ReglasCredito, ErrorCredito> err) return Result.err(err.error());
        var hoy = LocalDate.now(clock);
        p.actualizarMora(hoy, ((Result.Ok<ReglasCredito, ErrorCredito>) reglas).valor().tasaMoratoriaMaxima());
        var cuotas = p.cuotasVencidas().isEmpty() ? p.cuotaObjetivo().stream().toList() : p.cuotasVencidas();
        if (cuotas.isEmpty()) return Result.err(new ErrorCredito.NadaQuePagar());
        var total = cuotas.stream().map(Cuota::pendiente).reduce(Dinero.CERO, Dinero::sumar);
        var deApartado = cuotas.stream()
                .map(c -> c.getMontoApartado().menorQue(c.pendiente()) ? c.getMontoApartado() : c.pendiente())
                .reduce(Dinero.CERO, Dinero::sumar);
        return Result.ok(new APagar(p, cuotas, total, deApartado));
    }

    public Result<VistaAnticipado, ErrorCredito> prepararAnticipado(UsuarioId usuarioId) {
        var activo = prestamos.activoDe(usuarioId);
        if (activo.isEmpty()) return Result.err(new ErrorCredito.PrestamoNoEncontrado());
        var p = activo.get();
        var pendientes = (int) p.getCuotas().stream().filter(Cuota::esPendiente).count();
        return Result.ok(new VistaAnticipado(p.capitalPendiente(), pendientes, !p.cuotasVencidas().isEmpty(),
                UUID.randomUUID().toString()));
    }

    public Result<Dinero, ErrorCredito> pagarAnticipado(UsuarioId usuarioId, ModalidadPago modalidad, Dinero monto,
                                                        String referencia) {
        return ejecutor.ejecutarResultado(() -> {
            var activo = prestamos.activoDe(usuarioId);
            if (activo.isEmpty()) return Result.<Dinero, ErrorCredito>err(new ErrorCredito.PrestamoNoEncontrado());
            var p = activo.get();
            var aPagar = modalidad.montoACobrar(p, monto);
            var verificacion = p.verificarAnticipado(aPagar, modalidad);
            if (verificacion instanceof Result.Err<Void, ErrorCredito> err) return Result.<Dinero, ErrorCredito>err(err.error());

            var usarApartado = modalidad.apartadoAUsar(p);
            var cargo = billetera.cobrar(usuarioId, aPagar, usarApartado, true, true, referencia,
                    "Pago anticipado del crédito " + p.getCodigoCreditoEfa());
            if (cargo instanceof Result.Err<Cargo, ErrorCredito> err) return Result.<Dinero, ErrorCredito>err(err.error());
            var c = ((Result.Ok<Cargo, ErrorCredito>) cargo).valor();
            if (!c.nuevo()) return Result.<Dinero, ErrorCredito>ok(c.cobrado());
            var ahora = soporte.ahora();
            return p.aplicarAnticipado(c.cobrado(), modalidad, c.deApartado(), c.transaccionId(), ahora)
                    .flatMap(sobrante -> cierre.cerrar(p, sobrante)).map(pagado -> c.cobrado());
        });
    }

    public Result<Dinero, ErrorCredito> revocarCargo(UsuarioId usuarioId) {
        return ejecutor.ejecutarResultado(() -> {
            var activo = prestamos.activoDe(usuarioId);
            if (activo.isEmpty()) return Result.<Dinero, ErrorCredito>err(new ErrorCredito.PrestamoNoEncontrado());
            var p = activo.get();
            return p.revocarCargo(soporte.ahora()).flatMap(liberado ->
                    billetera.liberarApartado(usuarioId, liberado).map(ok -> {
                        prestamos.guardar(p);
                        return liberado;
                    }));
        });
    }
}
