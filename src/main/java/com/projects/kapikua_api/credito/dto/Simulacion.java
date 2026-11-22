package com.projects.kapikua_api.credito.dto;

import com.projects.kapikua_api.credito.model.CalculadoraFrancesa.CuotaProgramada;
import com.projects.kapikua_api.shared.Dinero;

import java.math.BigDecimal;
import java.util.List;

/** El cronograma simulado para un monto y un número de cuotas. */
public record Simulacion(OpcionesCredito opciones, Dinero monto, int cuotas, BigDecimal tea, BigDecimal tcea,
                         Dinero cuota, Dinero interesTotal, Dinero totalAPagar, List<CuotaProgramada> cronograma) {
}
