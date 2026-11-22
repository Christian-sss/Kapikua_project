package com.projects.kapikua_api.billetera.service;

import com.projects.kapikua_api.billetera.dto.Comprobante;
import com.projects.kapikua_api.billetera.dto.LineaCobro;
import com.projects.kapikua_api.billetera.dto.OperacionTitular;
import com.projects.kapikua_api.billetera.dto.ResumenBilletera;
import com.projects.kapikua_api.billetera.dto.ResumenCobros;
import com.projects.kapikua_api.billetera.repository.BilleteraRepository;
import com.projects.kapikua_api.billetera.repository.TransaccionRepository;
import com.projects.kapikua_api.billetera.error.ErrorBilletera;
import com.projects.kapikua_api.billetera.model.Billetera;
import com.projects.kapikua_api.billetera.model.Celular;
import com.projects.kapikua_api.billetera.model.Topes;
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

/** Consultas de la billetera: resumen, movimientos, comprobantes y datos que piden otros contextos. */
@Service
public class ConsultaBilleteraService {
    private static final int DIAS_POR_DEFECTO = 30;
    private static final int MAX_MOVIMIENTOS = 100;

    private final BilleteraRepository billeteras;
    private final TransaccionRepository transacciones;
    private final TopesService topes;
    private final ArmadorComprobantes comprobantes;
    private final Clock clock;

    public ConsultaBilleteraService(BilleteraRepository billeteras, TransaccionRepository transacciones,
                                    TopesService topes, ArmadorComprobantes comprobantes, Clock clock) {
        this.billeteras = billeteras;
        this.transacciones = transacciones;
        this.topes = topes;
        this.comprobantes = comprobantes;
        this.clock = clock;
    }

    public Result<ResumenBilletera, ErrorBilletera> resumen(UsuarioId usuarioId, LocalDate desde, LocalDate hasta) {
        var billetera = billeteras.porUsuario(usuarioId);
        if (billetera.isEmpty()) return Result.err(new ErrorBilletera.BilleteraNoEncontrada());
        var vigentes = topes.topesVigentes(LocalDate.now(clock));
        if (vigentes instanceof Result.Err<Topes, ErrorBilletera> err) return Result.err(err.error());
        var t = ((Result.Ok<Topes, ErrorBilletera>) vigentes).valor();

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
                acumulado.pagosYTransferencias(), acumulado.pagosYTransferencias().faltaPara(t.pagosMes()),
                t.porOperacion(), t.saldoMaximo(), inicio, fin, movimientos));
    }

    public boolean celularRegistrado(String celular) {
        return Celular.de(celular).map(billeteras::existeCelular).orElse(false);
    }

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
            var pagadores = (int) cobros.stream().map(LineaCobro::billeteraPagadora)
                    .distinct().count();
            return new ResumenCobros(semanas, total, semanasConCobros, cobros.size(), pagadores, diasAntiguedad);
        });
    }

    public Optional<UsuarioId> usuarioPorCelular(String celular) {
        return Celular.de(celular).flatMap(billeteras::porCelular).map(b -> b.getUsuarioId());
    }

    public Optional<UsuarioId> titularDe(Long billeteraId) {
        return billeteras.porId(billeteraId).map(b -> b.getUsuarioId());
    }

    public boolean bloqueada(Long billeteraId) {
        return billeteras.porId(billeteraId).map(Billetera::estaBloqueada).orElse(false);
    }

    public List<OperacionTitular> operacionesDelTitular(Long billeteraId, OffsetDateTime desde, OffsetDateTime hasta) {
        return transacciones.movimientos(billeteraId, desde, hasta, 1000).stream()
                .filter(m -> m.tipo().debeObservarse(m.esEntrada()))
                .map(m -> new OperacionTitular(m.transaccionId(), m.tipo().codigo(), m.montoAbsoluto(), m.fecha()))
                .sorted(Comparator.comparing(OperacionTitular::fecha))
                .toList();
    }

    public Optional<Comprobante> comprobante(UsuarioId usuarioId, Long transaccionId) {
        return transacciones.porId(transaccionId).flatMap(t -> comprobantes.paraUsuario(t, usuarioId));
    }
}
