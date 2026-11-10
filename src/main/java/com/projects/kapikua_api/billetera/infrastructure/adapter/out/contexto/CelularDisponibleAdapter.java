package com.projects.kapikua_api.billetera.infrastructure.adapter.out.contexto;

import com.projects.kapikua_api.billetera.application.port.in.ConsultarBilleteraUseCase;
import com.projects.kapikua_api.identidad.application.port.out.CelularDisponible;
import org.springframework.stereotype.Component;

/**
 * Implementa el puerto de Identidad desde Billetera, que es dueña del celular.
 * Vive aquí y no en Identidad para que la dependencia vaya en un solo sentido (Billetera → Identidad)
 * y no se forme un ciclo entre los dos contextos.
 */
@Component
class CelularDisponibleAdapter implements CelularDisponible {

    private final ConsultarBilleteraUseCase billeteras;

    CelularDisponibleAdapter(ConsultarBilleteraUseCase billeteras) {
        this.billeteras = billeteras;
    }

    @Override
    public boolean estaLibre(String celular) {
        return !billeteras.celularRegistrado(celular);
    }
}
