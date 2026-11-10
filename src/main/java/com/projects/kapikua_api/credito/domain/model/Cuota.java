package com.projects.kapikua_api.credito.domain.model;

import com.projects.kapikua_api.credito.domain.service.CalculadoraFrancesa;

import com.projects.kapikua_api.shared.Dinero;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
                                     LocalDate fechaPago, EstadoCuota estado) {
        return new Cuota(id, numero, fechaVencimiento, capital, interes, interesMoratorio, montoPagado, montoApartado,
                fechaPago, estado);
    }

    public Dinero total() {
        return capital.sumar(interes).sumar(interesMoratorio);
    }

    public Dinero pendiente() {
        return montoPagado.faltaPara(total());
    }

    public boolean porCobrar() {
        return estado == EstadoCuota.PENDIENTE || estado == EstadoCuota.VENCIDA;
    }

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

    public Imputacion imputar(Dinero monto) {
        if (monto.mayorQue(pendiente())) throw new IllegalArgumentException("El pago supera lo pendiente de la cuota");
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

    Dinero pagar(Dinero monto, Dinero deApartado, LocalDate hoy) {
        if (!porCobrar()) throw new IllegalStateException("La cuota " + numero + " no está por cobrar");
        if (monto.mayorQue(pendiente())) throw new IllegalArgumentException("El pago supera lo pendiente de la cuota");
        montoPagado = montoPagado.sumar(monto);
        montoApartado = montoApartado.mayorQue(deApartado) ? montoApartado.restar(deApartado) : Dinero.CERO;
        if (pendiente().esCero()) {
            estado = EstadoCuota.PAGADA;
            fechaPago = hoy;
            var sobrante = montoApartado;
            montoApartado = Dinero.CERO;
            return sobrante;
        }
        return Dinero.CERO;
    }

    void actualizarMoratorio(LocalDate hoy, BigDecimal tasaDiaria) {
        if (!vencidaAl(hoy)) return;
        var dias = ChronoUnit.DAYS.between(fechaVencimiento, hoy);
        var calculado = Dinero.de(capitalPendiente().valor().multiply(tasaDiaria).multiply(BigDecimal.valueOf(dias))
                .setScale(2, RoundingMode.HALF_EVEN));
        if (calculado.mayorQue(interesMoratorio)) interesMoratorio = calculado;
    }

    void vencer() {
        if (estado == EstadoCuota.PENDIENTE) estado = EstadoCuota.VENCIDA;
    }

    Dinero anular() {
        if (estado != EstadoCuota.PENDIENTE || !montoPagado.esCero()) {
            throw new IllegalStateException("Solo se anula una cuota pendiente sin pagos");
        }
        estado = EstadoCuota.ANULADA;
        var sobrante = montoApartado;
        montoApartado = Dinero.CERO;
        return sobrante;
    }

    void reprogramar(Dinero nuevoCapital, Dinero nuevoInteres) {
        if (estado != EstadoCuota.PENDIENTE || !montoPagado.esCero()) {
            throw new IllegalStateException("Solo se reprograma una cuota pendiente sin pagos");
        }
        capital = nuevoCapital;
        interes = nuevoInteres;
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
    public EstadoCuota getEstado() { return estado; }
}
