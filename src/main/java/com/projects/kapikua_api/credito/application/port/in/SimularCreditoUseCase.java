package com.projects.kapikua_api.credito.application.port.in;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.credito.domain.service.CalculadoraFrancesa.CuotaProgramada;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

import java.math.BigDecimal;
import java.util.List;

public interface SimularCreditoUseCase {
    Result<Opciones, ErrorCredito> opciones(UsuarioId usuarioId);

    Result<Simulacion, ErrorCredito> simular(UsuarioId usuarioId, Dinero monto, int cuotas);

    record Opciones(int nivel, int cuotasMinimas, int cuotasMaximas, BigDecimal tea, Dinero montoMaximo) {}

    record Simulacion(Opciones opciones, Dinero monto, int cuotas, BigDecimal tea, BigDecimal tcea, Dinero cuota,
                      Dinero interesTotal, Dinero totalAPagar, List<CuotaProgramada> cronograma) {}
}
