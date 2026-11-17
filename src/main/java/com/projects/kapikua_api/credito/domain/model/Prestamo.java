package com.projects.kapikua_api.credito.domain.model;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.credito.domain.event.AtrasoDetectado;
import com.projects.kapikua_api.credito.domain.event.PagoAplicado;
import com.projects.kapikua_api.credito.domain.event.PrestamoDesembolsado;
import com.projects.kapikua_api.credito.domain.event.PrestamoPagado;
import com.projects.kapikua_api.credito.domain.service.CalculadoraFrancesa;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.MatematicaFinanciera;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.domain.AgregadoRaiz;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class Prestamo extends AgregadoRaiz {
    public static final String CODIGO_ENTIDAD = "AYNI_SUR";
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final Long id;
    private final Long ofertaId;
    private final UsuarioId usuarioId;
    private final String codigoEntidad;
    private final String codigoCreditoEfa;
    private final Dinero monto;
    private final BigDecimal tea;
    private final BigDecimal tcea;
    private final int porcentajeRetencion;
    private final ConstanciaAceptacion constancia;
    private boolean cargoAutorizado;
    private final OffsetDateTime fechaAutorizacionCargo;
    private OffsetDateTime fechaRevocacionCargo;
    private EstadoPrestamo estado;
    private final OffsetDateTime fechaDesembolso;
    private final Long version;
    private final List<Cuota> cuotas;
    private final List<Retencion> retencionesNuevas = new ArrayList<>();
    private final List<Pago> pagosNuevos = new ArrayList<>();
    private boolean conPagosDeAval;

    private Prestamo(Long id, Long ofertaId, UsuarioId usuarioId, String codigoEntidad, String codigoCreditoEfa,
                     Dinero monto, BigDecimal tea, BigDecimal tcea, int porcentajeRetencion,
                     ConstanciaAceptacion constancia, boolean cargoAutorizado, OffsetDateTime fechaAutorizacionCargo,
                     OffsetDateTime fechaRevocacionCargo, EstadoPrestamo estado, OffsetDateTime fechaDesembolso,
                     Long version, List<Cuota> cuotas, boolean conPagosDeAval) {
        this.id = id;
        this.conPagosDeAval = conPagosDeAval;
        this.ofertaId = Objects.requireNonNull(ofertaId);
        this.usuarioId = Objects.requireNonNull(usuarioId);
        this.codigoEntidad = Objects.requireNonNull(codigoEntidad);
        this.codigoCreditoEfa = Objects.requireNonNull(codigoCreditoEfa);
        this.monto = Objects.requireNonNull(monto);
        this.tea = Objects.requireNonNull(tea);
        this.tcea = Objects.requireNonNull(tcea);
        this.porcentajeRetencion = porcentajeRetencion;
        this.constancia = Objects.requireNonNull(constancia);
        this.cargoAutorizado = cargoAutorizado;
        this.fechaAutorizacionCargo = fechaAutorizacionCargo;
        this.fechaRevocacionCargo = fechaRevocacionCargo;
        this.estado = Objects.requireNonNull(estado);
        this.fechaDesembolso = Objects.requireNonNull(fechaDesembolso);
        this.version = version;
        this.cuotas = new ArrayList<>(cuotas);
        this.cuotas.sort(Comparator.comparingInt(Cuota::getNumero));
    }

    public static Prestamo desembolsar(Oferta oferta, String codigoCreditoEfa, int porcentajeRetencion,
                                       ConstanciaAceptacion constancia, OffsetDateTime ahora, ZoneId zona) {
        if (!oferta.estaAceptada()) throw new IllegalStateException("La oferta no está aceptada");
        var c = oferta.getCondiciones();
        var cronograma = CalculadoraFrancesa.cronograma(c.monto(), c.tea(), c.cuotas(),
                ahora.atZoneSameInstant(zona).toLocalDate());
        var p = new Prestamo(null, oferta.getId(), oferta.getUsuarioId(), CODIGO_ENTIDAD, codigoCreditoEfa, c.monto(),
                c.tea(), c.tcea(), porcentajeRetencion, constancia, true, constancia.fecha(), null,
                EstadoPrestamo.VIGENTE, ahora, null, cronograma.cuotas().stream().map(Cuota::programada).toList(), false);
        p.registrar(new PrestamoDesembolsado(p.usuarioId, p.ofertaId, codigoCreditoEfa, c.monto()));
        return p;
    }

    public static Prestamo reconstituir(Long id, Long ofertaId, UsuarioId usuarioId, String codigoEntidad,
                                        String codigoCreditoEfa, Dinero monto, BigDecimal tea, BigDecimal tcea,
                                        int porcentajeRetencion, ConstanciaAceptacion constancia,
                                        boolean cargoAutorizado, OffsetDateTime fechaAutorizacionCargo,
                                        OffsetDateTime fechaRevocacionCargo, String estado,
                                        OffsetDateTime fechaDesembolso, Long version, List<Cuota> cuotas,
                                        boolean conPagosDeAval) {
        return new Prestamo(id, ofertaId, usuarioId, codigoEntidad, codigoCreditoEfa, monto, tea, tcea,
                porcentajeRetencion, constancia, cargoAutorizado, fechaAutorizacionCargo, fechaRevocacionCargo,
                EstadoPrestamo.desdeNombre(estado), fechaDesembolso, version, cuotas, conPagosDeAval);
    }

    public boolean estaActivo() {
        return estado.estaActivo();
    }

    public boolean estaPagado() {
        return estado.estaPagado();
    }

    public Dinero capitalPendiente() {
        return cuotas.stream().filter(Cuota::porCobrar).map(Cuota::capitalPendiente).reduce(Dinero.CERO, Dinero::sumar);
    }

    public Optional<Cuota> cuotaObjetivo() {
        return cuotas.stream().filter(Cuota::porCobrar).findFirst();
    }

    public List<Cuota> cuotasVencidas() {
        return cuotas.stream().filter(Cuota::estaVencida).toList();
    }

    boolean tieneCuotasVencidas() {
        return cuotas.stream().anyMatch(Cuota::estaVencida);
    }

    public List<Cuota> cuotasPorCobrarAl(LocalDate hoy) {
        return cuotas.stream().filter(c -> c.porCobrar() && !c.getFechaVencimiento().isAfter(hoy)).toList();
    }

    public int diasAtraso(LocalDate hoy) {
        return cuotas.stream().filter(c -> c.vencidaAl(hoy))
                .mapToInt(c -> (int) ChronoUnit.DAYS.between(c.getFechaVencimiento(), hoy))
                .max().orElse(0);
    }

    public Dinero totalApartado() {
        return cuotas.stream().map(Cuota::getMontoApartado).reduce(Dinero.CERO, Dinero::sumar);
    }

    public Dinero montoARetener(Dinero cobro) {
        if (!cargoAutorizado || !estaActivo()) return Dinero.CERO;
        var objetivo = cuotaObjetivo();
        if (objetivo.isEmpty()) return Dinero.CERO;
        var porcentaje = Dinero.de(cobro.valor().multiply(BigDecimal.valueOf(porcentajeRetencion))
                .divide(CIEN, 2, RoundingMode.HALF_EVEN));
        var falta = objetivo.get().faltaPorApartar();
        return porcentaje.menorQue(falta) ? porcentaje : falta;
    }

    public Result<Dinero, ErrorCredito> registrarRetencion(Long transaccionVentaId, Dinero apartado, OffsetDateTime ahora) {
        return estado.verificarCobro().flatMap(ignorado -> {
            if (apartado.esCero()) return Result.ok(apartado);
            var cuota = cuotaObjetivo();
            if (cuota.isEmpty()) return Result.err(new ErrorCredito.NadaQuePagar());
            cuota.get().apartar(apartado);
            retencionesNuevas.add(new Retencion(cuota.get().getId(), transaccionVentaId, Origen.DEUDOR, apartado, ahora));
            return Result.ok(apartado);
        });
    }

    public Result<Dinero, ErrorCredito> registrarRetencionDeAval(Long cuotaId, Long transaccionVentaId, Dinero monto,
                                                                  OffsetDateTime ahora) {
        return estado.verificarCobro().map(ignorado -> {
            retencionesNuevas.add(new Retencion(cuotaId, transaccionVentaId, Origen.AVAL, monto, ahora));
            return monto;
        });
    }

    public Dinero totalVencido() {
        return cuotasVencidas().stream().map(Cuota::pendiente).reduce(Dinero.CERO, Dinero::sumar);
    }

    public Result<Dinero, ErrorCredito> aplicarPago(List<Cuota> destino, Dinero monto, boolean usaApartado,
                                                     Long transaccionId, Origen origen, boolean automatico,
                                                     LocalDate hoy, OffsetDateTime ahora) {
        return estado.verificarCobro()
                .flatMap(ignorado -> ModalidadPago.REGULAR.aplicarRegular(this, destino, monto, usaApartado,
                        transaccionId, origen, automatico, hoy, ahora))
                .map(sobrante -> {
                    actualizarEstado(ahora);
                    return sobrante;
                });
    }

    void registrarPagoCuota(Pago pago) {
        if (pago.origen() == Origen.AVAL) conPagosDeAval = true;
        registrarPago(pago);
    }

    private void registrarPago(Pago pago) {
        pagosNuevos.add(pago);
        registrar(new PagoAplicado(usuarioId, id, codigoCreditoEfa, pago.tipo(), pago.origen(), pago.monto()));
    }

    public void actualizarMora(LocalDate hoy, BigDecimal tasaMoratoriaAnual) {
        if (!estaActivo()) return;
        var tasaDiaria = MatematicaFinanciera.tasaDiaria(tasaMoratoriaAnual);
        cuotas.forEach(c -> c.actualizarMoratorio(hoy, tasaDiaria));
    }

    public void vencerImpagas(LocalDate hoy, OffsetDateTime ahora) {
        if (!estaActivo()) return;
        cuotas.stream().filter(Cuota::esPendiente).filter(c -> !c.getFechaVencimiento().isAfter(hoy))
                .forEach(c -> c.vencer(hoy, ahora));
        actualizarEstado(ahora);
    }

    public int revisarAtraso(LocalDate hoy, OffsetDateTime ahora) {
        if (!estaActivo()) return 0;
        vencerImpagas(hoy, ahora);
        var dias = diasAtraso(hoy);
        if (dias > 0) registrar(new AtrasoDetectado(usuarioId, id, ofertaId, dias));
        return dias;
    }

    public Result<CambioEstado, ErrorCredito> marcarEnMora(OffsetDateTime ahora) {
        return estado.marcarEnMora(this, ahora);
    }

    public Result<CambioEstado, ErrorCredito> cerrar(OffsetDateTime ahora) {
        return estado.cerrar(this, ahora);
    }

    CambioEstado cambiarEstado(EstadoPrestamo nuevo, String motivo, OffsetDateTime ahora) {
        var cambio = new CambioEstado(estado.nombre(), nuevo.nombre(), motivo, ahora);
        estado = nuevo;
        return cambio;
    }

    Result<CambioEstado, ErrorCredito> cerrarSiSaldado(OffsetDateTime ahora) {
        if (cuotaObjetivo().isPresent()) return Result.err(new ErrorCredito.PrestamoConSaldoPendiente());
        var cambio = cambiarEstado(EstadoPrestamo.PAGADO, "Se saldó el préstamo", ahora);
        registrar(new PrestamoPagado(usuarioId, id, ofertaId, conPagosDeAval));
        return Result.ok(cambio);
    }

    private void actualizarEstado(OffsetDateTime ahora) {
        if (cuotaObjetivo().isEmpty()) cerrar(ahora);
        else if (tieneCuotasVencidas()) marcarEnMora(ahora);
        else estado.ponerAlDia(this, ahora);
    }

    public Result<Void, ErrorCredito> verificarAnticipado(Dinero montoPago, ModalidadPago modalidad) {
        return estado.verificarAnticipado(this)
                .flatMap(ignorado -> modalidad.verificarAnticipado(this, montoPago));
    }

    public Dinero montoAnticipadoTotal() {
        return capitalPendiente();
    }

    public Result<Dinero, ErrorCredito> aplicarAnticipado(Dinero montoPago, ModalidadPago modalidad, Dinero deApartado,
                                                           Long transaccionId, OffsetDateTime ahora) {
        return estado.verificarAnticipado(this)
                .flatMap(ignorado -> modalidad.aplicarAnticipado(this, montoPago, deApartado, ahora))
                .map(sobrante -> {
                    registrarPago(new Pago(null, transaccionId, TipoPago.ANTICIPADO, Origen.DEUDOR, false, modalidad, montoPago,
                            Imputacion.soloCapital(montoPago), ahora));
                    actualizarEstado(ahora);
                    return sobrante;
                });
    }

    public Result<Dinero, ErrorCredito> revocarCargo(OffsetDateTime ahora) {
        if (!estaActivo()) return Result.err(new ErrorCredito.PrestamoNoEncontrado());
        if (!cargoAutorizado) return Result.err(new ErrorCredito.CargoNoAutorizado());
        cargoAutorizado = false;
        fechaRevocacionCargo = ahora;
        var liberado = Dinero.CERO;
        for (var c : cuotas) liberado = liberado.sumar(c.liberarApartado());
        return Result.ok(liberado);
    }

    public List<Retencion> retencionesNuevas() { return Collections.unmodifiableList(retencionesNuevas); }
    public List<Pago> pagosNuevos() { return Collections.unmodifiableList(pagosNuevos); }

    public Long getId() { return id; }
    public Long getOfertaId() { return ofertaId; }
    public UsuarioId getUsuarioId() { return usuarioId; }
    public String getCodigoEntidad() { return codigoEntidad; }
    public String getCodigoCreditoEfa() { return codigoCreditoEfa; }
    public Dinero getMonto() { return monto; }
    public BigDecimal getTea() { return tea; }
    public BigDecimal getTcea() { return tcea; }
    public int getPorcentajeRetencion() { return porcentajeRetencion; }
    public ConstanciaAceptacion getConstancia() { return constancia; }
    public boolean isCargoAutorizado() { return cargoAutorizado; }
    public OffsetDateTime getFechaAutorizacionCargo() { return fechaAutorizacionCargo; }
    public OffsetDateTime getFechaRevocacionCargo() { return fechaRevocacionCargo; }
    public String getEstado() { return estado.nombre(); }
    public OffsetDateTime getFechaDesembolso() { return fechaDesembolso; }
    public Long getVersion() { return version; }
    public boolean isConPagosDeAval() { return conPagosDeAval; }
    public List<Cuota> getCuotas() { return Collections.unmodifiableList(cuotas); }
}
