package com.projects.kapikua_api.credito.dto;

import com.projects.kapikua_api.credito.model.CondicionesCredito;
import com.projects.kapikua_api.shared.Dinero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** La oferta del cliente con lo que puede hacer con ella. */
public record DetalleOferta(Long id, String estado, String recomendacion, String motivo,
                            Dinero monto, int cuotas, BigDecimal tea, Dinero cuota, boolean condicionesConfirmadas,
                            boolean requiereAval, List<AvalVista> avales, LocalDate vigenteHasta,
                            String motivoDenegacion, boolean puedeDesignarAval, boolean puedeEnviarAEntidad,
                            boolean puedeAceptar, CondicionesCredito condicionesEfa) {
}
