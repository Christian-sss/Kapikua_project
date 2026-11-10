package com.projects.kapikua_api.credito.domain.model;

import com.projects.kapikua_api.credito.domain.service.CalculadoraFrancesa;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.credito.domain.event.AtrasoDetectado;
import com.projects.kapikua_api.credito.domain.event.PagoAplicado;
import com.projects.kapikua_api.credito.domain.event.PrestamoDesembolsado;
import com.projects.kapikua_api.credito.domain.event.PrestamoPagado;
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

/**
 * Raíz de agregado: el préstamo aceptado y desembolsado. La deuda oficial es de la entidad aliada; esta es
 * la copia que Kapikua muestra y cobra, con las condiciones aceptadas y la constancia (N6, RN-20).
 * Las retenciones y los pagos nuevos se acumulan para que el repositorio los inserte (son de solo inserción).
 * El dinero lo mueve Billetera: aquí solo se decide cuánto y a qué cuota se aplica.
 * Registra sus eventos de dominio: desembolso, cada pago aplicado, el atraso y el pago total.
 */
public final class Prestamo extends AgregadoRaiz {

    /** La única entidad aliada de la v1 (puerto EntidadFinanciera). */
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
    /** RN-32: si el aval pagó alguna cuota (en este u otro momento). */
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

    /**
     * CU-10, paso 8: crea el préstamo VIGENTE con su cronograma, a partir de la oferta aceptada.
     * El cargo automático queda autorizado con la misma fecha de la aceptación (RN-22).
     */
    public static Prestamo desembolsar(Oferta oferta, String codigoCreditoEfa, int porcentajeRetencion,
                                       ConstanciaAceptacion constancia, OffsetDateTime ahora, ZoneId zona) {
        if (oferta.getEstado() != EstadoOferta.ACEPTADA) throw new IllegalStateException("La oferta no está aceptada");
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
                                        OffsetDateTime fechaRevocacionCargo, EstadoPrestamo estado,
                                        OffsetDateTime fechaDesembolso, Long version, List<Cuota> cuotas,
                                        boolean conPagosDeAval) {
        return new Prestamo(id, ofertaId, usuarioId, codigoEntidad, codigoCreditoEfa, monto, tea, tcea,
                porcentajeRetencion, constancia, cargoAutorizado, fechaAutorizacionCargo, fechaRevocacionCargo,
                estado, fechaDesembolso, version, cuotas, conPagosDeAval);
    }

    // ------------------------------------------------------------------ consultas

    public boolean estaActivo() {
        return estado == EstadoPrestamo.VIGENTE || estado == EstadoPrestamo.EN_MORA;
    }

    /** Capital que falta pagar: el de las cuotas por cobrar. */
    public Dinero capitalPendiente() {
        return cuotas.stream().filter(Cuota::porCobrar).map(Cuota::capitalPendiente).reduce(Dinero.CERO, Dinero::sumar);
    }

    /** La próxima cuota que se debe: la vencida más antigua o, si no hay, la siguiente pendiente. */
    public Optional<Cuota> cuotaObjetivo() {
        return cuotas.stream().filter(Cuota::porCobrar).findFirst();
    }

    public List<Cuota> cuotasVencidas() {
        return cuotas.stream().filter(c -> c.getEstado() == EstadoCuota.VENCIDA).toList();
    }

    /** CU-13, paso 2: las que vencen hoy y las ya vencidas. */
    public List<Cuota> cuotasPorCobrarAl(LocalDate hoy) {
        return cuotas.stream().filter(c -> c.porCobrar() && !c.getFechaVencimiento().isAfter(hoy)).toList();
    }

    /** Días de atraso de la cuota vencida más antigua; 0 si no hay atraso. */
    public int diasAtraso(LocalDate hoy) {
        return cuotas.stream().filter(c -> c.vencidaAl(hoy))
                .mapToInt(c -> (int) ChronoUnit.DAYS.between(c.getFechaVencimiento(), hoy))
                .max().orElse(0);
    }

    public Dinero totalApartado() {
        return cuotas.stream().map(Cuota::getMontoApartado).reduce(Dinero.CERO, Dinero::sumar);
    }

    // ------------------------------------------------------------------ retención de ventas (RN-22)

    /**
     * Cuánto apartar de un cobro: el porcentaje autorizado, solo hasta cubrir la cuota objetivo.
     * Cero si el cargo automático fue revocado o no hay cuota por cobrar.
     */
    public Dinero montoARetener(Dinero cobro) {
        if (!cargoAutorizado || !estaActivo()) return Dinero.CERO;
        var objetivo = cuotaObjetivo();
        if (objetivo.isEmpty()) return Dinero.CERO;
        var porcentaje = Dinero.de(cobro.valor().multiply(BigDecimal.valueOf(porcentajeRetencion))
                .divide(CIEN, 2, RoundingMode.HALF_EVEN));
        var falta = objetivo.get().faltaPorApartar();
        return porcentaje.menorQue(falta) ? porcentaje : falta;
    }

    /** Registra lo que Billetera efectivamente apartó para la cuota objetivo. */
    public void registrarRetencion(Long transaccionVentaId, Dinero apartado, OffsetDateTime ahora) {
        if (apartado.esCero()) return;
        var cuota = cuotaObjetivo().orElseThrow();
        cuota.apartar(apartado);
        retencionesNuevas.add(new Retencion(cuota.getId(), transaccionVentaId, Origen.DEUDOR, apartado, ahora));
    }

    /** RN-25: lo retenido de una venta del aval, que se aplica de inmediato a las cuotas vencidas. */
    public void registrarRetencionDeAval(Long cuotaId, Long transaccionVentaId, Dinero monto, OffsetDateTime ahora) {
        retencionesNuevas.add(new Retencion(cuotaId, transaccionVentaId, Origen.AVAL, monto, ahora));
    }

    public Dinero totalVencido() {
        return cuotasVencidas().stream().map(Cuota::pendiente).reduce(Dinero.CERO, Dinero::sumar);
    }

    // ------------------------------------------------------------------ pagos de cuotas (RN-23)

    /**
     * Aplica {@code monto} a las cuotas dadas, en orden, cada una hasta lo que debe. Si {@code usaApartado}, cada cuota
     * consume primero lo retenido para ella (así lo cobra Billetera). Registra un Pago por cuota con su imputación.
     * @return lo apartado que sobró en cuotas que quedaron pagadas (hay que liberarlo en la billetera)
     */
    public Dinero aplicarPago(List<Cuota> destino, Dinero monto, boolean usaApartado, Long transaccionId, Origen origen,
                              boolean automatico, LocalDate hoy, OffsetDateTime ahora) {
        var resto = monto;
        var sobrante = Dinero.CERO;
        for (var cuota : destino) {
            if (resto.esCero()) break;
            if (!cuota.porCobrar()) continue;
            var aEsta = resto.menorQue(cuota.pendiente()) ? resto : cuota.pendiente();
            if (aEsta.esCero()) continue;
            var deApartado = usaApartado ? (cuota.getMontoApartado().menorQue(aEsta) ? cuota.getMontoApartado() : aEsta)
                                         : Dinero.CERO;
            var imputacion = cuota.imputar(aEsta);
            sobrante = sobrante.sumar(cuota.pagar(aEsta, deApartado, hoy));
            pagosNuevos.add(new Pago(cuota.getId(), transaccionId, TipoPago.CUOTA, origen, automatico, null, aEsta,
                    imputacion, ahora));
            if (origen == Origen.AVAL) conPagosDeAval = true;
            registrar(new PagoAplicado(usuarioId, id, codigoCreditoEfa, TipoPago.CUOTA, origen, aEsta));
            resto = resto.restar(aEsta);
        }
        if (!resto.esCero()) throw new IllegalArgumentException("El pago supera lo que se debe en esas cuotas");
        actualizarEstado();
        return sobrante;
    }

    // ------------------------------------------------------------------ cobro diario (CU-13, RN-24)

    /** Suma el interés moratorio del día a las cuotas atrasadas, sin capitalizar (RN-24). */
    public void actualizarMora(LocalDate hoy, BigDecimal tasaMoratoriaAnual) {
        var tasaDiaria = MatematicaFinanciera.tasaDiaria(tasaMoratoriaAnual);
        cuotas.forEach(c -> c.actualizarMoratorio(hoy, tasaDiaria));
    }

    /** Las cuotas que vencieron sin cubrirse pasan a VENCIDA; el préstamo, a EN_MORA. */
    public void vencerImpagas(LocalDate hoy) {
        cuotas.stream().filter(c -> c.porCobrar() && !c.getFechaVencimiento().isAfter(hoy)).forEach(Cuota::vencer);
        actualizarEstado();
    }

    /**
     * CU-13, 5a y 5b: vence las cuotas impagas y, si hay atraso, lo registra (Evaluación lo usa para el nivel y
     * Créditos para escalar al aval). Se llama una vez por día. Devuelve los días de atraso.
     */
    public int revisarAtraso(LocalDate hoy) {
        vencerImpagas(hoy);
        var dias = diasAtraso(hoy);
        if (dias > 0) registrar(new AtrasoDetectado(usuarioId, id, ofertaId, dias));
        return dias;
    }

    private void actualizarEstado() {
        var antes = estado;
        if (cuotas.stream().noneMatch(Cuota::porCobrar)) estado = EstadoPrestamo.PAGADO;
        else if (cuotas.stream().anyMatch(c -> c.getEstado() == EstadoCuota.VENCIDA)) estado = EstadoPrestamo.EN_MORA;
        else estado = EstadoPrestamo.VIGENTE;
        if (estado == EstadoPrestamo.PAGADO && antes != EstadoPrestamo.PAGADO) {
            registrar(new PrestamoPagado(usuarioId, id, ofertaId, conPagosDeAval));
        }
    }

    // ------------------------------------------------------------------ pago anticipado (RN-27)

    /** Verificaciones antes de cobrar un pago anticipado. */
    public Result<Void, ErrorCredito> verificarAnticipado(Dinero montoPago, ModalidadPago modalidad) {
        if (!estaActivo()) return Result.err(new ErrorCredito.PrestamoNoEncontrado());
        if (!cuotasVencidas().isEmpty()) return Result.err(new ErrorCredito.HayCuotasVencidas());
        var capital = capitalPendiente();
        if (modalidad == ModalidadPago.TOTAL) return Result.ok();
        if (montoPago.esCero() || !montoPago.menorQue(capital)) {
            return Result.err(new ErrorCredito.MontoAnticipadoInvalido(capital.toString()));
        }
        return Result.ok();
    }

    /** Lo que cobra un pago anticipado total: el capital pendiente, sin intereses futuros ni penalidad. */
    public Dinero montoAnticipadoTotal() {
        return capitalPendiente();
    }

    /**
     * Aplica un pago anticipado ya cobrado. TOTAL anula las cuotas pendientes y cierra el préstamo; los parciales
     * reprograman: REDUCIR_CUOTA mantiene el número de cuotas y REDUCIR_PLAZO mantiene la cuota.
     * @param deApartado lo que el cobro tomó de lo retenido para esas cuotas
     * @return lo apartado que hay que liberar en la billetera
     */
    public Dinero aplicarAnticipado(Dinero montoPago, ModalidadPago modalidad, Dinero deApartado, Long transaccionId,
                                    OffsetDateTime ahora) {
        var pendientes = cuotas.stream().filter(c -> c.getEstado() == EstadoCuota.PENDIENTE).toList();
        var sobrante = Dinero.CERO;
        if (modalidad == ModalidadPago.TOTAL) {
            for (var c : pendientes) sobrante = sobrante.sumar(c.anular());
            sobrante = sobrante.mayorQue(deApartado) ? sobrante.restar(deApartado) : Dinero.CERO;
        } else {
            var i = MatematicaFinanciera.tasaSemanal(tea);
            var nuevoSaldo = capitalPendiente().restar(montoPago);
            var cuotaFija = modalidad == ModalidadPago.REDUCIR_CUOTA
                    ? MatematicaFinanciera.cuota(nuevoSaldo, i, pendientes.size())
                    : pendientes.getFirst().getCapital().sumar(pendientes.getFirst().getInteres());
            var n = modalidad == ModalidadPago.REDUCIR_CUOTA ? pendientes.size()
                    : CalculadoraFrancesa.plazoPara(nuevoSaldo, i, cuotaFija, pendientes.size());
            var tramos = CalculadoraFrancesa.amortizar(nuevoSaldo, i, cuotaFija, n);
            for (int k = 0; k < pendientes.size(); k++) {
                if (k < n) pendientes.get(k).reprogramar(tramos.get(k).capital(), tramos.get(k).interes());
                else sobrante = sobrante.sumar(pendientes.get(k).anular());
            }
        }
        pagosNuevos.add(new Pago(null, transaccionId, TipoPago.ANTICIPADO, Origen.DEUDOR, false, modalidad, montoPago,
                Imputacion.soloCapital(montoPago), ahora));
        registrar(new PagoAplicado(usuarioId, id, codigoCreditoEfa, TipoPago.ANTICIPADO, Origen.DEUDOR, montoPago));
        actualizarEstado();
        return sobrante;
    }

    // ------------------------------------------------------------------ cargo automático (RN-22)

    /** La autorización es revocable: se dejan de retener ventas y lo apartado vuelve a estar disponible. */
    public Result<Dinero, ErrorCredito> revocarCargo(OffsetDateTime ahora) {
        if (!estaActivo()) return Result.err(new ErrorCredito.PrestamoNoEncontrado());
        if (!cargoAutorizado) return Result.err(new ErrorCredito.CargoNoAutorizado());
        cargoAutorizado = false;
        fechaRevocacionCargo = ahora;
        var liberado = Dinero.CERO;
        for (var c : cuotas) liberado = liberado.sumar(c.liberarApartado());
        return Result.ok(liberado);
    }

    // ------------------------------------------------------------------ acceso

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
    public EstadoPrestamo getEstado() { return estado; }
    public OffsetDateTime getFechaDesembolso() { return fechaDesembolso; }
    public Long getVersion() { return version; }
    public boolean isConPagosDeAval() { return conPagosDeAval; }
    public List<Cuota> getCuotas() { return Collections.unmodifiableList(cuotas); }
}
