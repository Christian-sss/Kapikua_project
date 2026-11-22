package com.projects.kapikua_api.credito.model;

import com.projects.kapikua_api.credito.error.ErrorCredito;
import com.projects.kapikua_api.credito.model.CalculadoraFrancesa;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class Cuota {
    private final Long id;
    private final int numero;
    private final LocalDate fechaVencimiento;
    private Dinero capital;
    private Dinero interes;
    private Dinero interesMoratorio;
    private Dinero montoPagado;
    private Dinero montoApartado;
    private LocalDate fechaPago;
    private EstadoCuota estado;

    private Cuota(Long id, int numero, LocalDate fechaVencimiento, Dinero capital, Dinero interes,
                  Dinero interesMoratorio, Dinero montoPagado, Dinero montoApartado, LocalDate fechaPago,
                  EstadoCuota estado) {
        this.id = id;
        this.numero = numero;
        this.fechaVencimiento = Objects.requireNonNull(fechaVencimiento);
        this.capital = Objects.requireNonNull(capital);
        this.interes = Objects.requireNonNull(interes);
        this.interesMoratorio = Objects.requireNonNull(interesMoratorio);
        this.montoPagado = Objects.requireNonNull(montoPagado);
        this.montoApartado = Objects.requireNonNull(montoApartado);
        this.fechaPago = fechaPago;
        this.estado = Objects.requireNonNull(estado);
    }

    static Cuota programada(CalculadoraFrancesa.CuotaProgramada c) {
        return new Cuota(null, c.numero(), c.vencimiento(), c.capital(), c.interes(), Dinero.CERO, Dinero.CERO,
                Dinero.CERO, null, EstadoCuota.PENDIENTE);
    }

    public static Cuota reconstituir(Long id, int numero, LocalDate fechaVencimiento, Dinero capital, Dinero interes,
                                     Dinero interesMoratorio, Dinero montoPagado, Dinero montoApartado,
                                     LocalDate fechaPago, String estado) {
        return new Cuota(id, numero, fechaVencimiento, capital, interes, interesMoratorio, montoPagado, montoApartado,
                fechaPago, EstadoCuota.desdeNombre(estado));
    }

    public Dinero total() {
        return capital.sumar(interes).sumar(interesMoratorio);
    }

    public Dinero pendiente() {
        return montoPagado.faltaPara(total());
    }

    public boolean porCobrar() {
        return estado.porCobrar();
    }

    public boolean esPendiente() { return estado.esPendiente(); }

    public boolean estaVencida() { return estado.estaVencida(); }

    public boolean vencidaAl(LocalDate hoy) {
        return porCobrar() && hoy.isAfter(fechaVencimiento);
    }

    public Dinero capitalPendiente() {
        var cubiertoAntes = interesMoratorio.sumar(interes);
        var aCapital = montoPagado.mayorQue(cubiertoAntes) ? montoPagado.restar(cubiertoAntes) : Dinero.CERO;
        return aCapital.faltaPara(capital);
    }

    public Dinero faltaPorApartar() {
        return montoApartado.faltaPara(pendiente());
    }

    record PagoImputado(Imputacion imputacion, Dinero sobrante) {}

    Result<PagoImputado, ErrorCredito> imputar(Dinero monto, Dinero deApartado, LocalDate hoy) {
        return estado.imputar(this, monto, deApartado, hoy);
    }

    private Imputacion calcularImputacion(Dinero monto) {
        var pagado = montoPagado;
        var moratorioPendiente = pagado.faltaPara(interesMoratorio);
        var tras = pagado.mayorQue(interesMoratorio) ? pagado.restar(interesMoratorio) : Dinero.CERO;
        var interesPendiente = tras.faltaPara(interes);

        var resto = monto;
        var aMoratorio = minimo(resto, moratorioPendiente);
        resto = resto.restar(aMoratorio);
        var aInteres = minimo(resto, interesPendiente);
        resto = resto.restar(aInteres);
        return new Imputacion(aMoratorio, aInteres, resto);
    }

    void apartar(Dinero monto) {
        montoApartado = montoApartado.sumar(monto);
    }

    Result<PagoImputado, ErrorCredito> aplicarImputacion(Dinero monto, Dinero deApartado, LocalDate hoy) {
        Objects.requireNonNull(hoy);
        if (monto.esCero() || monto.mayorQue(pendiente())) {
            return Result.err(new ErrorCredito.PagoCuotaInvalido(numero));
        }
        var imputacion = calcularImputacion(monto);
        var apartadoRestante = montoApartado.mayorQue(deApartado) ? montoApartado.restar(deApartado) : Dinero.CERO;
        montoPagado = montoPagado.sumar(monto);
        montoApartado = apartadoRestante;
        if (!pendiente().esCero()) {
            return Result.ok(new PagoImputado(imputacion, Dinero.CERO));
        }
        estado = EstadoCuota.PAGADA;
        fechaPago = hoy;
        return Result.ok(new PagoImputado(imputacion, liberarApartado()));
    }

    void actualizarMoratorio(LocalDate hoy, BigDecimal tasaDiaria) {
        if (!vencidaAl(hoy)) return;
        var dias = ChronoUnit.DAYS.between(fechaVencimiento, hoy);
        var calculado = Dinero.de(capitalPendiente().valor().multiply(tasaDiaria).multiply(BigDecimal.valueOf(dias))
                .setScale(2, RoundingMode.HALF_EVEN));
        if (calculado.mayorQue(interesMoratorio)) interesMoratorio = calculado;
    }

    Result<CambioEstado, ErrorCredito> vencer(LocalDate hoy, OffsetDateTime ahora) {
        return estado.vencer(this, hoy, ahora);
    }

    Result<CambioEstado, ErrorCredito> anular(OffsetDateTime ahora) {
        return estado.anular(this, ahora);
    }

    CambioEstado aplicarAnulacion(OffsetDateTime ahora) {
        var cambio = cambiarEstado(EstadoCuota.ANULADA, "Cuota anulada por pago anticipado", ahora);
        liberarApartado();
        return cambio;
    }

    CambioEstado cambiarEstado(EstadoCuota nuevo, String motivo, OffsetDateTime ahora) {
        var cambio = new CambioEstado(estado.nombre(), nuevo.nombre(), motivo, ahora);
        estado = nuevo;
        return cambio;
    }

    Result<Void, ErrorCredito> verificarReprogramable() {
        return estado.verificarReprogramable(this);
    }

    Result<Void, ErrorCredito> reprogramar(Dinero nuevoCapital, Dinero nuevoInteres) {
        Objects.requireNonNull(nuevoCapital);
        Objects.requireNonNull(nuevoInteres);
        return verificarReprogramable().map(ignorado -> {
            capital = nuevoCapital;
            interes = nuevoInteres;
            return null;
        });
    }

    Dinero liberarApartado() {
        var liberado = montoApartado;
        montoApartado = Dinero.CERO;
        return liberado;
    }

    private static Dinero minimo(Dinero a, Dinero b) {
        return a.menorQue(b) ? a : b;
    }

    public Long getId() { return id; }
    public int getNumero() { return numero; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public Dinero getCapital() { return capital; }
    public Dinero getInteres() { return interes; }
    public Dinero getInteresMoratorio() { return interesMoratorio; }
    public Dinero getMontoPagado() { return montoPagado; }
    public Dinero getMontoApartado() { return montoApartado; }
    public LocalDate getFechaPago() { return fechaPago; }
    public String getEstado() { return estado.nombre(); }
}
