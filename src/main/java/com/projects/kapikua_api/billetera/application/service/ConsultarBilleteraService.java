package com.projects.kapikua_api.billetera.application.service;

import com.projects.kapikua_api.billetera.application.port.in.Comprobante;
import com.projects.kapikua_api.billetera.application.port.in.ConsultarBilleteraUseCase;
import com.projects.kapikua_api.billetera.application.port.out.BilleteraRepositoryPort;
import com.projects.kapikua_api.billetera.application.port.out.ParametrosRegulatorios;
import com.projects.kapikua_api.billetera.application.port.out.TransaccionRepositoryPort;
import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.billetera.domain.model.Celular;
import com.projects.kapikua_api.billetera.domain.model.TipoTransaccion;
import com.projects.kapikua_api.billetera.domain.model.Topes;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import org.springframework.stereotype.Service;

import java.time.Clock;
import com.projects.kapikua_api.shared.Dinero;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

@Service
public class ConsultarBilleteraService implements ConsultarBilleteraUseCase {
    private static final int DIAS_POR_DEFECTO = 30;
    private static final int MAX_MOVIMIENTOS = 100;

    private final BilleteraRepositoryPort billeteras;
    private final TransaccionRepositoryPort transacciones;
    private final ParametrosRegulatorios parametros;
    private final ArmadorComprobantes comprobantes;
    private final Clock clock;

    ConsultarBilleteraService(BilleteraRepositoryPort billeteras, TransaccionRepositoryPort transacciones,
                              ParametrosRegulatorios parametros, ArmadorComprobantes comprobantes, Clock clock) {
        this.billeteras = billeteras;
        this.transacciones = transacciones;
        this.parametros = parametros;
        this.comprobantes = comprobantes;
        this.clock = clock;
    }

    @Override
    public Result<ResumenBilletera, ErrorBilletera> resumen(UsuarioId usuarioId, LocalDate desde, LocalDate hasta) {
        var billetera = billeteras.porUsuario(usuarioId);
        if (billetera.isEmpty()) return Result.err(new ErrorBilletera.BilleteraNoEncontrada());
        var topes = parametros.topesVigentes(LocalDate.now(clock));
        if (topes instanceof Result.Err<Topes, ErrorBilletera> err) return Result.err(err.error());
        var t = ((Result.Ok<Topes, ErrorBilletera>) topes).valor();

        var b = billetera.get();
        var mes = PeriodoMensual.actual(clock);
        var acumulado = transacciones.acumulado(b.getId(), mes.desde(), mes.hasta());

        var fin = hasta != null ? hasta : LocalDate.now(clock);
        var inicio = desde != null ? desde : fin.minusDays(DIAS_POR_DEFECTO);
        if (inicio.isAfter(fin)) inicio = fin;
        var zona = clock.getZone();
        var movimientos = transacciones.movimientos(b.getId(),
                inicio.atStartOfDay(zona).toOffsetDateTime(),
                fin.plusDays(1).atStartOfDay(zona).toOffsetDateTime(), MAX_MOVIMIENTOS);

        return Result.ok(new ResumenBilletera(
                b.getCelular().numero(), b.getNombreNegocio(),
                b.getSaldo(), b.getSaldoApartado(), b.saldoDisponible(),
                acumulado.recargas(), acumulado.recargas().faltaPara(t.recargasMes()),
                acumulado.pagosYTransferencias(), acumulado.pagosYTransferencias().faltaPara(t.pagosMes()),
                t.porOperacion(), t.saldoMaximo(), inicio, fin, movimientos));
    }

    @Override
    public boolean celularRegistrado(String celular) {
        return Celular.de(celular).map(billeteras::existeCelular).orElse(false);
    }

    @Override
    public Optional<ResumenCobros> cobros(UsuarioId usuarioId, LocalDate fechaSolicitud, int semanasPeriodo) {
        return billeteras.porUsuario(usuarioId).map(b -> {
            var zona = clock.getZone();
            var apertura = b.getFechaApertura().atZoneSameInstant(zona).toLocalDate();
            var hasta = fechaSolicitud.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            var primerLunes = apertura.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
            var desde = hasta.minusWeeks(semanasPeriodo);
            if (desde.isBefore(primerLunes)) desde = primerLunes;
            var semanas = desde.isBefore(hasta) ? (int) ChronoUnit.WEEKS.between(desde, hasta) : 0;
            var diasAntiguedad = Math.max(0, ChronoUnit.DAYS.between(apertura, fechaSolicitud));
            if (semanas == 0) return new ResumenCobros(0, Dinero.CERO, 0, 0, 0, diasAntiguedad);

            var inicio = desde;
            var cobros = transacciones.cobrosRecibidos(b.getId(),
                    desde.atStartOfDay(zona).toOffsetDateTime(), hasta.atStartOfDay(zona).toOffsetDateTime());
            var total = cobros.stream().map(c -> Dinero.de(c.monto())).reduce(Dinero.CERO, Dinero::sumar);
            var semanasConCobros = (int) cobros.stream()
                    .map(c -> ChronoUnit.WEEKS.between(inicio, c.fecha().atZoneSameInstant(zona).toLocalDate()))
                    .distinct().count();
            var pagadores = (int) cobros.stream().map(TransaccionRepositoryPort.LineaCobro::billeteraPagadora)
                    .distinct().count();
            return new ResumenCobros(semanas, total, semanasConCobros, cobros.size(), pagadores, diasAntiguedad);
        });
    }

    @Override
    public Optional<UsuarioId> usuarioPorCelular(String celular) {
        return Celular.de(celular).flatMap(billeteras::porCelular).map(b -> b.getUsuarioId());
    }

    @Override
    public Optional<UsuarioId> titularDe(Long billeteraId) {
        return billeteras.porId(billeteraId).map(b -> b.getUsuarioId());
    }

    @Override
    public boolean bloqueada(Long billeteraId) {
        return billeteras.porId(billeteraId).map(b -> !b.estaActiva()).orElse(false);
    }

    @Override
    public List<OperacionTitular> operacionesDelTitular(Long billeteraId, OffsetDateTime desde, OffsetDateTime hasta) {
        return transacciones.movimientos(billeteraId, desde, hasta, 1000).stream()
                .filter(m -> m.tipo() == TipoTransaccion.RECARGA ? m.esEntrada()
                        : (m.tipo() == TipoTransaccion.TRANSFERENCIA || m.tipo() == TipoTransaccion.PAGO_NEGOCIO) && !m.esEntrada())
                .map(m -> new OperacionTitular(m.transaccionId(), m.tipo().name(), m.montoAbsoluto(), m.fecha()))
                .sorted(Comparator.comparing(OperacionTitular::fecha))
                .toList();
    }

    @Override
    public Optional<Comprobante> comprobante(UsuarioId usuarioId, Long transaccionId) {
        return transacciones.porId(transaccionId).flatMap(t -> comprobantes.paraUsuario(t, usuarioId));
    }
}
