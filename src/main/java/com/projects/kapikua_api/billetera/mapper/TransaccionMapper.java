package com.projects.kapikua_api.billetera.mapper;

import com.projects.kapikua_api.billetera.entity.MovimientoEntity;
import com.projects.kapikua_api.billetera.entity.TransaccionEntity;
import com.projects.kapikua_api.billetera.model.TiposTransaccion;
import com.projects.kapikua_api.billetera.model.Transaccion;
import com.projects.kapikua_api.shared.Dinero;

/** Data Mapper entre las filas de transacción y movimiento y el modelo Transaccion. */
public final class TransaccionMapper {
    private TransaccionMapper() {
    }

    public static Transaccion aModelo(TransaccionEntity e) {
        return new Transaccion(e.getId(), TiposTransaccion.desdeCodigo(e.getTipo()), Dinero.de(e.getMonto()),
                e.getReferencia(), e.getDescripcion(), e.getFecha(),
                e.getMovimientos().stream()
                        .map(m -> new Transaccion.Movimiento(m.getBilleteraId(), m.getMonto(), Dinero.de(m.getSaldoPosterior())))
                        .toList());
    }

    public static TransaccionEntity aEntidad(Transaccion t) {
        var e = new TransaccionEntity();
        e.setTipo(t.tipo().codigo());
        e.setMonto(t.monto().valor());
        e.setReferencia(t.referencia());
        e.setDescripcion(t.descripcion());
        e.setFecha(t.fecha());
        t.movimientos().forEach(m -> e.getMovimientos().add(
                new MovimientoEntity(null, m.billeteraId(), m.monto(), m.saldoPosterior().valor(), t.fecha())));
        return e;
    }
}
