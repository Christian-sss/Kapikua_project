package com.projects.kapikua_api.billetera.service;

import com.projects.kapikua_api.billetera.dto.Comprobante;
import com.projects.kapikua_api.billetera.repository.BilleteraRepository;
import com.projects.kapikua_api.billetera.model.Billetera;
import com.projects.kapikua_api.billetera.model.TipoTransaccion;
import com.projects.kapikua_api.billetera.model.Transaccion;
import com.projects.kapikua_api.identidad.service.IdentidadService;
import com.projects.kapikua_api.shared.UsuarioId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class ArmadorComprobantes {
    private final BilleteraRepository billeteras;
    private final IdentidadService identidad;

    ArmadorComprobantes(BilleteraRepository billeteras, IdentidadService identidad) {
        this.billeteras = billeteras;
        this.identidad = identidad;
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

    Optional<Comprobante> paraOrigen(Transaccion t, UsuarioId usuario) {
        return paraUsuario(t, usuario).filter(c -> billeteras.porUsuario(usuario)
                .flatMap(b -> t.movimientoDe(b.getId()))
                .map(m -> m.monto().signum() < 0)
                .orElse(false));
    }

    String contraparte(TipoTransaccion tipo, Billetera otra, boolean esEntrada) {
        if (!esEntrada) {
            var descripcion = tipo.descripcion(otra);
            if (descripcion != null) return descripcion;
        }
        return identidad.nombreEnmascarado(otra.getUsuarioId()) + " · " + otra.getCelular().enmascarado();
    }
}
