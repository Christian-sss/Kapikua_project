package com.projects.kapikua_api.billetera.mapper;

import com.projects.kapikua_api.billetera.entity.BilleteraEntity;
import com.projects.kapikua_api.billetera.model.Billetera;
import com.projects.kapikua_api.billetera.model.Celular;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.UsuarioId;

/** Data Mapper entre la fila de la tabla (BilleteraEntity) y el modelo con reglas (Billetera). */
public final class BilleteraMapper {
    private BilleteraMapper() {
    }

    public static Billetera aModelo(BilleteraEntity e) {
        return Billetera.reconstituir(e.getId(), new UsuarioId(e.getUsuarioId()), new Celular(e.getNumeroCelular()),
                e.getNombreNegocio(), Dinero.de(e.getSaldo()), Dinero.de(e.getSaldoApartado()), e.getEstado(),
                e.getMotivoBloqueo(), e.getFechaApertura(), e.getVersion());
    }

    public static BilleteraEntity aEntidadNueva(Billetera b) {
        var e = new BilleteraEntity();
        e.setUsuarioId(b.getUsuarioId().valor());
        e.setNumeroCelular(b.getCelular().numero());
        e.setNombreNegocio(b.getNombreNegocio());
        e.setFechaApertura(b.getFechaApertura());
        copiarCambios(b, e);
        return e;
    }

    /** Copia a la fila solo lo que el modelo puede cambiar: saldos y estado. */
    public static void copiarCambios(Billetera b, BilleteraEntity e) {
        e.setSaldo(b.getSaldo().valor());
        e.setSaldoApartado(b.getSaldoApartado().valor());
        e.setEstado(b.getEstado());
        e.setMotivoBloqueo(b.getMotivoBloqueo());
    }
}
