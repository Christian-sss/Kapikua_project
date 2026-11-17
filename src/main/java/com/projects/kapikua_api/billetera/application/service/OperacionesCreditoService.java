package com.projects.kapikua_api.billetera.application.service;

import com.projects.kapikua_api.billetera.application.port.in.OperacionesCreditoUseCase;
import com.projects.kapikua_api.billetera.application.port.in.OperacionesCreditoUseCase.Cargo;
import com.projects.kapikua_api.billetera.application.port.out.BilleteraRepositoryPort;
import com.projects.kapikua_api.billetera.application.port.out.ParametrosRegulatorios;
import com.projects.kapikua_api.billetera.application.port.out.TransaccionRepositoryPort;
import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.billetera.domain.model.AcumuladoMensual;
import com.projects.kapikua_api.billetera.domain.model.Billetera;
import com.projects.kapikua_api.billetera.domain.model.TiposTransaccion;
import com.projects.kapikua_api.billetera.domain.model.Topes;
import com.projects.kapikua_api.billetera.domain.model.Transaccion;
import com.projects.kapikua_api.billetera.domain.service.TopesSBS;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.puerto.PublicadorEventos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Service
public class OperacionesCreditoService implements OperacionesCreditoUseCase {
    private final BilleteraRepositoryPort billeteras;
    private final TransaccionRepositoryPort transacciones;
    private final ParametrosRegulatorios parametros;
    private final PublicadorEventos publicador;
    private final Clock clock;

    public OperacionesCreditoService(BilleteraRepositoryPort billeteras, TransaccionRepositoryPort transacciones,
                                     ParametrosRegulatorios parametros, PublicadorEventos publicador, Clock clock) {
        this.billeteras = billeteras;
        this.transacciones = transacciones;
        this.parametros = parametros;
        this.publicador = publicador;
        this.clock = clock;
    }

    @Override
    public Result<Void, ErrorBilletera> verificarIngreso(UsuarioId usuarioId, Dinero monto) {
        return prepararDesembolso(usuarioId, monto).map(ingreso -> null);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Result<Long, ErrorBilletera> acreditarDesembolso(UsuarioId usuarioId, Dinero monto, String referencia,
                                                            String descripcion) {
        var existente = transacciones.porReferencia(referencia);
        if (existente.isPresent()) return Result.ok(existente.get().id());

        var validacion = prepararDesembolso(usuarioId, monto);
        if (validacion instanceof Result.Err<Ingreso, ErrorBilletera> err) return Result.err(err.error());
        var ingreso = ((Result.Ok<Ingreso, ErrorBilletera>) validacion).valor();
        var abono = ingreso.billetera().acreditar(monto, ingreso.saldoMaximo(), false);
        if (abono instanceof Result.Err<Void, ErrorBilletera> err) return Result.err(err.error());

        var guardada = billeteras.guardar(ingreso.billetera());
        var ahora = OffsetDateTime.now(clock);
        var transaccion = transacciones.registrar(Transaccion.desembolso(guardada, monto, referencia, descripcion, ahora));
        publicador.publicarTodos(transaccion.eventos());
        return Result.ok(transaccion.id());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Result<Dinero, ErrorBilletera> apartar(UsuarioId usuarioId, Dinero monto) {
        var billetera = billeteras.porUsuario(usuarioId);
        if (billetera.isEmpty()) return Result.err(new ErrorBilletera.BilleteraNoEncontrada());
        return billetera.get().apartar(monto).map(apartado -> {
            if (!apartado.esCero()) billeteras.guardar(billetera.get());
            return apartado;
        });
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Result<Void, ErrorBilletera> liberarApartado(UsuarioId usuarioId, Dinero monto) {
        if (monto.esCero()) return Result.ok();
        var billetera = billeteras.porUsuario(usuarioId);
        if (billetera.isEmpty()) return Result.err(new ErrorBilletera.BilleteraNoEncontrada());
        return billetera.get().liberarApartado(monto).map(liberado -> {
            if (!liberado.esCero()) billeteras.guardar(billetera.get());
            return null;
        });
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Result<Cargo, ErrorBilletera> cobrar(UsuarioId usuarioId, Dinero monto, Dinero usarApartado, boolean exigirTodo,
                                                boolean anticipado, String referencia, String descripcion) {
        var existente = transacciones.porReferencia(referencia);
        if (existente.isPresent()) {
            return Result.ok(new Cargo(existente.get().id(), existente.get().monto(), Dinero.CERO, false));
        }
        var encontrada = billeteras.porUsuario(usuarioId);
        if (encontrada.isEmpty()) return Result.err(new ErrorBilletera.BilleteraNoEncontrada());
        var b = encontrada.get();
        var salida = b.validarSalida();
        if (salida instanceof Result.Err<Void, ErrorBilletera> err) return Result.err(err.error());

        var deApartado = menor(menor(usarApartado, b.getSaldoApartado()), monto);
        var deDisponible = menor(monto.restar(deApartado), b.saldoDisponible());
        var cobrado = deApartado.sumar(deDisponible);
        if (exigirTodo && cobrado.menorQue(monto)) return Result.err(new ErrorBilletera.SaldoInsuficiente());
        if (cobrado.esCero()) return Result.ok(Cargo.NADA);

        var tipo = anticipado ? TiposTransaccion.PAGO_ANTICIPADO : TiposTransaccion.PAGO_CUOTA;
        var topes = parametros.topesVigentes(LocalDate.now(clock));
        if (topes instanceof Result.Err<Topes, ErrorBilletera> err) return Result.err(err.error());
        var t = ((Result.Ok<Topes, ErrorBilletera>) topes).valor();
        var limite = TopesSBS.validar(tipo, b, cobrado, AcumuladoMensual.VACIO, t);
        if (limite instanceof Result.Err<Void, ErrorBilletera> err) return Result.err(err.error());

        var consumo = b.consumirApartado(deApartado);
        if (consumo instanceof Result.Err<Dinero, ErrorBilletera> err) return Result.err(err.error());
        if (!deDisponible.esCero()) {
            var debito = b.debitar(deDisponible);
            if (debito instanceof Result.Err<Void, ErrorBilletera> err) return Result.err(err.error());
        }
        var guardada = billeteras.guardar(b);
        var ahora = OffsetDateTime.now(clock);
        var transaccion = transacciones.registrar(Transaccion.pagoACredito(tipo, guardada, cobrado, referencia,
                descripcion, ahora));
        publicador.publicarTodos(transaccion.eventos());
        return Result.ok(new Cargo(transaccion.id(), cobrado, deApartado, true));
    }

    private static Dinero menor(Dinero a, Dinero b) {
        return a.menorQue(b) ? a : b;
    }

    private record Ingreso(Billetera billetera, Dinero saldoMaximo) {}

    private Result<Ingreso, ErrorBilletera> prepararDesembolso(UsuarioId usuarioId, Dinero monto) {
        var billetera = billeteras.porUsuario(usuarioId);
        if (billetera.isEmpty()) return Result.err(new ErrorBilletera.BilleteraNoEncontrada());
        var topes = parametros.topesVigentes(LocalDate.now(clock));
        if (topes instanceof Result.Err<Topes, ErrorBilletera> err) return Result.err(err.error());
        var t = ((Result.Ok<Topes, ErrorBilletera>) topes).valor();
        var b = billetera.get();
        return TopesSBS.validar(TiposTransaccion.DESEMBOLSO, b, monto, AcumuladoMensual.VACIO, t)
                .map(ok -> new Ingreso(b, t.saldoMaximo()));
    }
}
