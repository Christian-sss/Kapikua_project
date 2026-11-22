package com.projects.kapikua_api.credito.service;

import com.projects.kapikua_api.credito.client.OperacionesBilletera;
import com.projects.kapikua_api.credito.error.ErrorCredito;
import com.projects.kapikua_api.credito.model.Prestamo;
import com.projects.kapikua_api.credito.repository.OfertaRepository;
import com.projects.kapikua_api.credito.repository.PrestamoRepository;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * Último paso de todo cobro: devuelve el apartado que sobró, guarda el préstamo y avisa de sus pagos. Si con ese
 * cobro el préstamo quedó pagado, libera al aval en la misma transacción y avisa que se terminó de pagar.
 * Recibe siempre un préstamo que estaba activo al leerlo, así que si ahora está pagado es porque se saldó aquí.
 */
@Component
class CierreCobranza {
    private final PrestamoRepository prestamos;
    private final OfertaRepository ofertas;
    private final OperacionesBilletera billetera;
    private final PublicadorCredito publicador;
    private final Clock clock;

    CierreCobranza(PrestamoRepository prestamos, OfertaRepository ofertas, OperacionesBilletera billetera,
                   PublicadorCredito publicador, Clock clock) {
        this.prestamos = prestamos;
        this.ofertas = ofertas;
        this.billetera = billetera;
        this.publicador = publicador;
        this.clock = clock;
    }

    Result<Boolean, ErrorCredito> cerrar(Prestamo p, Dinero apartadoSobrante) {
        var liberacion = apartadoSobrante.esCero() ? Result.<ErrorCredito>ok()
                : billetera.liberarApartado(p.getUsuarioId(), apartadoSobrante);
        return liberacion.map(ok -> {
            prestamos.guardar(p);
            publicador.pagosRegistrados(p);
            if (p.estaPagado()) {
                liberarAval(p);
                publicador.prestamoPagado(p);
            }
            return p.estaPagado();
        });
    }

    private void liberarAval(Prestamo p) {
        var oferta = ofertas.porId(p.getOfertaId()).orElseThrow();
        oferta.liberarAval(OffsetDateTime.now(clock));
        ofertas.guardar(oferta);
    }
}
