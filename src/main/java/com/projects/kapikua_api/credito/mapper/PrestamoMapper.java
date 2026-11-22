package com.projects.kapikua_api.credito.mapper;

import com.projects.kapikua_api.credito.entity.CuotaEntity;
import com.projects.kapikua_api.credito.entity.PagoEntity;
import com.projects.kapikua_api.credito.entity.PrestamoEntity;
import com.projects.kapikua_api.credito.entity.RetencionEntity;
import com.projects.kapikua_api.credito.model.ConstanciaAceptacion;
import com.projects.kapikua_api.credito.model.Cuota;
import com.projects.kapikua_api.credito.model.Pago;
import com.projects.kapikua_api.credito.model.Prestamo;
import com.projects.kapikua_api.credito.model.Retencion;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.UsuarioId;

import java.math.BigDecimal;

/** Data Mapper entre las filas de préstamo, cuota, pago y retención y el modelo Prestamo. */
public final class PrestamoMapper {
    private PrestamoMapper() {
    }

    public static Prestamo aModelo(PrestamoEntity e, boolean conPagosDeAval) {
        return Prestamo.reconstituir(e.getId(), e.getOfertaId(), new UsuarioId(e.getUsuarioId()),
                e.getCodigoEntidad(), e.getCodigoCreditoEfa(), Dinero.de(e.getMonto()), e.getTea(), e.getTcea(),
                e.getPorcentajeRetencion().intValue(),
                new ConstanciaAceptacion(e.getFechaAceptacion(), e.getDispositivoAceptacion(),
                        e.getVersionHojaResumen()),
                e.isCargoAutorizado(), e.getFechaAutorizacionCargo(), e.getFechaRevocacionCargo(), e.getEstado(),
                e.getFechaDesembolso(), e.getVersion(), e.getCuotas().stream().map(PrestamoMapper::aModelo).toList(),
                conPagosDeAval);
    }

    private static Cuota aModelo(CuotaEntity c) {
        return Cuota.reconstituir(c.getId(), c.getNumero(), c.getFechaVencimiento(), Dinero.de(c.getCapital()),
                Dinero.de(c.getInteres()), Dinero.de(c.getInteresMoratorio()), Dinero.de(c.getMontoPagado()),
                Dinero.de(c.getMontoApartado()), c.getFechaPago(), c.getEstado());
    }

    public static PrestamoEntity aEntidadNueva(Prestamo p) {
        var e = new PrestamoEntity();
        e.setOfertaId(p.getOfertaId());
        e.setUsuarioId(p.getUsuarioId().valor());
        e.setCodigoEntidad(p.getCodigoEntidad());
        e.setCodigoCreditoEfa(p.getCodigoCreditoEfa());
        e.setMonto(p.getMonto().valor());
        e.setTea(p.getTea());
        e.setTcea(p.getTcea());
        e.setPorcentajeRetencion(BigDecimal.valueOf(p.getPorcentajeRetencion()));
        e.setVersionHojaResumen(p.getConstancia().versionHojaResumen());
        e.setFechaAceptacion(p.getConstancia().fecha());
        e.setDispositivoAceptacion(p.getConstancia().dispositivo());
        e.setFechaDesembolso(p.getFechaDesembolso());
        e.setFechaAutorizacionCargo(p.getFechaAutorizacionCargo());
        copiarDatosQueCambian(p, e);
        for (var c : p.getCuotas()) {
            var fila = new CuotaEntity();
            fila.setNumero(c.getNumero());
            fila.setFechaVencimiento(c.getFechaVencimiento());
            copiarCambios(c, fila);
            e.getCuotas().add(fila);
        }
        return e;
    }

    /** Copia a las filas lo que el modelo puede cambiar: cargo, estado y el avance de cada cuota. */
    public static void copiarCambios(Prestamo p, PrestamoEntity e) {
        copiarDatosQueCambian(p, e);
        for (var c : p.getCuotas()) {
            var fila = e.getCuotas().stream().filter(x -> x.getId().equals(c.getId())).findFirst().orElseThrow();
            copiarCambios(c, fila);
        }
    }

    public static RetencionEntity aEntidad(Retencion r) {
        var e = new RetencionEntity();
        e.setCuotaId(r.cuotaId());
        e.setTransaccionId(r.transaccionVentaId());
        e.setOrigen(r.origen());
        e.setMonto(r.monto().valor());
        e.setFecha(r.fecha());
        return e;
    }

    public static PagoEntity aEntidad(Long prestamoId, Pago pago) {
        var e = new PagoEntity();
        e.setPrestamoId(prestamoId);
        e.setCuotaId(pago.cuotaId());
        e.setTransaccionId(pago.transaccionId());
        e.setTipo(pago.tipo());
        e.setOrigen(pago.origen());
        e.setAutomatico(pago.automatico());
        e.setModalidad(pago.modalidad().esAnticipada() ? pago.modalidad().codigo() : null);
        e.setMonto(pago.monto().valor());
        e.setImputadoMoratorio(pago.imputacion().moratorio().valor());
        e.setImputadoInteres(pago.imputacion().interes().valor());
        e.setImputadoCapital(pago.imputacion().capital().valor());
        e.setFecha(pago.fecha());
        return e;
    }

    private static void copiarDatosQueCambian(Prestamo p, PrestamoEntity e) {
        e.setCargoAutorizado(p.isCargoAutorizado());
        e.setFechaRevocacionCargo(p.getFechaRevocacionCargo());
        e.setEstado(p.getEstado());
    }

    private static void copiarCambios(Cuota c, CuotaEntity fila) {
        fila.setCapital(c.getCapital().valor());
        fila.setInteres(c.getInteres().valor());
        fila.setInteresMoratorio(c.getInteresMoratorio().valor());
        fila.setMontoPagado(c.getMontoPagado().valor());
        fila.setMontoApartado(c.getMontoApartado().valor());
        fila.setFechaPago(c.getFechaPago());
        fila.setEstado(c.getEstado());
    }
}
