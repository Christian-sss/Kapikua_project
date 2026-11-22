package com.projects.kapikua_api.credito.mapper;

import com.projects.kapikua_api.credito.entity.AvalEntity;
import com.projects.kapikua_api.credito.entity.OfertaEntity;
import com.projects.kapikua_api.credito.model.Aval;
import com.projects.kapikua_api.credito.model.CicloGarantia;
import com.projects.kapikua_api.credito.model.CondicionesCredito;
import com.projects.kapikua_api.credito.model.Invitacion;
import com.projects.kapikua_api.credito.model.Oferta;
import com.projects.kapikua_api.credito.model.Respuesta;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.UsuarioId;

import java.math.BigDecimal;

/** Data Mapper entre las filas de oferta y aval y el modelo Oferta con sus avales. */
public final class OfertaMapper {
    private OfertaMapper() {
    }

    public static Oferta aModelo(OfertaEntity e) {
        var condiciones = e.getMonto() == null ? null : new CondicionesCredito(Dinero.de(e.getMonto()),
                e.getNumeroCuotas(), e.getTea(), e.getTcea(), Dinero.de(e.getMontoCuota()));
        return Oferta.reconstituir(e.getId(), e.getEvaluacionId(), new UsuarioId(e.getUsuarioId()),
                e.getCodigoSolicitudEfa(), e.getEstado(), condiciones, e.getMotivoDenegacion(), e.getFecha(),
                e.getVigenteHasta(), e.getVersion(), e.getAvales().stream().map(OfertaMapper::aModelo).toList());
    }

    private static Aval aModelo(AvalEntity a) {
        return Aval.reconstituir(a.getId(), a.getEstado(),
                new Invitacion(new UsuarioId(a.getUsuarioDeudorId()), new UsuarioId(a.getUsuarioAvalId()),
                        a.getFechaInvitacion()),
                new Respuesta(a.getFechaRespuesta(), a.getDispositivoAceptacion(),
                        a.getPorcentajeRetencionAutorizado() == null ? null
                                : a.getPorcentajeRetencionAutorizado().intValue()),
                new CicloGarantia(a.getFechaAviso(), a.getFechaActivacion(), a.getFechaLiberacion()));
    }

    public static OfertaEntity aEntidadNueva(Oferta o) {
        var e = new OfertaEntity();
        e.setEvaluacionId(o.getEvaluacionId());
        e.setUsuarioId(o.getUsuarioId().valor());
        e.setFecha(o.getFecha());
        e.setVigenteHasta(o.getVigenteHasta());
        copiarCambios(o, e);
        return e;
    }

    /** Copia a las filas lo que el modelo puede cambiar: estado, condiciones de la EFA y avales (agrega los nuevos). */
    public static void copiarCambios(Oferta o, OfertaEntity e) {
        e.setCodigoSolicitudEfa(o.getCodigoSolicitudEfa());
        e.setEstado(o.getEstado());
        if (o.getCondiciones() != null) {
            var c = o.getCondiciones();
            e.setMonto(c.monto().valor());
            e.setNumeroCuotas(c.cuotas());
            e.setTea(c.tea());
            e.setTcea(c.tcea());
            e.setMontoCuota(c.cuota().valor());
        }
        e.setMotivoDenegacion(o.getMotivoDenegacion());
        for (var a : o.getAvales()) {
            var existente = a.getId() == null ? null
                    : e.getAvales().stream().filter(x -> x.getId().equals(a.getId())).findFirst().orElse(null);
            var fila = existente != null ? existente : new AvalEntity();
            if (existente == null) {
                fila.setUsuarioDeudorId(a.getUsuarioDeudorId().valor());
                fila.setUsuarioAvalId(a.getUsuarioAvalId().valor());
                fila.setFechaInvitacion(a.getFechaInvitacion());
                e.getAvales().add(fila);
            }
            fila.setEstado(a.getEstado());
            fila.setPorcentajeRetencionAutorizado(a.getPorcentajeRetencionAutorizado() == null ? null
                    : BigDecimal.valueOf(a.getPorcentajeRetencionAutorizado()));
            fila.setFechaRespuesta(a.getFechaRespuesta());
            fila.setDispositivoAceptacion(a.getDispositivoAceptacion());
            fila.setFechaAviso(a.getFechaAviso());
            fila.setFechaActivacion(a.getFechaActivacion());
            fila.setFechaLiberacion(a.getFechaLiberacion());
        }
    }
}
