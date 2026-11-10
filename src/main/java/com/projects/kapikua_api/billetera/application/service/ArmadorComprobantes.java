package com.projects.kapikua_api.billetera.application.service;

import com.projects.kapikua_api.billetera.application.port.in.Comprobante;
import com.projects.kapikua_api.billetera.application.port.out.BilleteraRepositoryPort;
import com.projects.kapikua_api.billetera.application.port.out.Titulares;
import com.projects.kapikua_api.billetera.domain.model.Billetera;
import com.projects.kapikua_api.billetera.domain.model.TipoTransaccion;
import com.projects.kapikua_api.billetera.domain.model.Transaccion;
import com.projects.kapikua_api.shared.UsuarioId;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Arma el comprobante de una transacción visto por uno de sus participantes.
 * Vacío si el usuario no participa: nadie ve operaciones ajenas.
 */
@Component
class ArmadorComprobantes {

    private final BilleteraRepositoryPort billeteras;
    private final Titulares titulares;

    ArmadorComprobantes(BilleteraRepositoryPort billeteras, Titulares titulares) {
        this.billeteras = billeteras;
        this.titulares = titulares;
    }

    Optional<Comprobante> paraUsuario(Transaccion t, UsuarioId usuario) {
        var propia = billeteras.porUsuario(usuario);
        var movimiento = propia.flatMap(b -> t.movimientoDe(b.getId()));
        if (movimiento.isEmpty()) return Optional.empty();
        var esEntrada = movimiento.get().monto().signum() > 0;
        var otra = t.movimientos().stream()
                .filter(m -> !m.billeteraId().equals(propia.get().getId()))
                .findFirst()
                .flatMap(m -> billeteras.porId(m.billeteraId()));
        var contraparte = otra.map(o -> contraparte(t.tipo(), o, esEntrada)).orElse("Tarjeta");
        return Optional.of(new Comprobante(t.id(), t.tipo(), t.monto(), t.referencia(), contraparte, t.fecha(),
                movimiento.get().saldoPosterior()));
    }

    /** Solo si el usuario es quien envió el dinero (para devolver el comprobante de un reintento). */
    Optional<Comprobante> paraOrigen(Transaccion t, UsuarioId usuario) {
        return paraUsuario(t, usuario).filter(c -> billeteras.porUsuario(usuario)
                .flatMap(b -> t.movimientoDe(b.getId()))
                .map(m -> m.monto().signum() < 0)
                .orElse(false));
    }

    String contraparte(TipoTransaccion tipo, Billetera otra, boolean esEntrada) {
        if (tipo == TipoTransaccion.PAGO_NEGOCIO && !esEntrada) return otra.getNombreNegocio();
        return titulares.nombreEnmascarado(otra.getUsuarioId()) + " · " + otra.getCelular().enmascarado();
    }
}
