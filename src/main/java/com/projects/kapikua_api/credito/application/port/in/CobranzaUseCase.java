package com.projects.kapikua_api.credito.application.port.in;

import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.UsuarioId;

import java.time.LocalDate;

public interface CobranzaUseCase {
    void aplicarRetencion(UsuarioId usuarioId, Long transaccionVentaId, Dinero cobro);

    ResumenCobro ejecutarCobroDiario(LocalDate hoy);

    record ResumenCobro(int ofertasActualizadas, int prestamosRevisados, int cuotasCobradas, Dinero totalCobrado,
                        int prestamosEnMora, int avisosAval, int avalesActivados, int prestamosPagados, int errores) {}
}
