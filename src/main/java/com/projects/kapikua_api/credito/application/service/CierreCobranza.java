package com.projects.kapikua_api.credito.application.service;

import com.projects.kapikua_api.credito.application.port.out.OperacionesBilletera;
import com.projects.kapikua_api.credito.application.port.out.PrestamoRepositoryPort;
import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.credito.domain.model.Prestamo;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
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

    Result<Boolean, ErrorCredito> cerrar(Prestamo p, Dinero apartadoSobrante) {
        var liberacion = apartadoSobrante.esCero() ? Result.<ErrorCredito>ok()
                : billetera.liberarApartado(p.getUsuarioId(), apartadoSobrante);
        return liberacion.map(ok -> {
            prestamos.guardar(p);
            publicador.publicarDe(p);
            return p.estaPagado();
        });
    }
}
