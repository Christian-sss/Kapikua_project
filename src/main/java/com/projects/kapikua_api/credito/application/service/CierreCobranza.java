package com.projects.kapikua_api.credito.application.service;

import com.projects.kapikua_api.credito.application.port.out.OperacionesBilletera;
import com.projects.kapikua_api.credito.application.port.out.PrestamoRepositoryPort;
import com.projects.kapikua_api.credito.domain.model.EstadoPrestamo;
import com.projects.kapikua_api.credito.domain.model.Prestamo;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.puerto.PublicadorEventos;
import org.springframework.stereotype.Component;

@Component
class CierreCobranza {
    private final PrestamoRepositoryPort prestamos;
    private final OperacionesBilletera billetera;
    private final PublicadorEventos publicador;

    CierreCobranza(PrestamoRepositoryPort prestamos, OperacionesBilletera billetera, PublicadorEventos publicador) {
        this.prestamos = prestamos;
        this.billetera = billetera;
        this.publicador = publicador;
    }

    boolean cerrar(Prestamo p, Dinero apartadoSobrante) {
        if (!apartadoSobrante.esCero()) billetera.liberarApartado(p.getUsuarioId(), apartadoSobrante);
        prestamos.guardar(p);
        publicador.publicarDe(p);
        return p.getEstado() == EstadoPrestamo.PAGADO;
    }
}
