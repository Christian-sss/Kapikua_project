package com.projects.kapikua_api.credito.dto;

import com.projects.kapikua_api.evaluacion.domain.model.Destino;
import com.projects.kapikua_api.shared.Dinero;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record SolicitudCreditoForm(

        @NotNull(message = "Ingresa el monto")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero")
        @Digits(integer = 7, fraction = 2, message = "Usa hasta 2 decimales")
        BigDecimal monto,

        @NotNull(message = "Elige el numero de cuotas")
        @Min(value = 1, message = "Elige el número de cuotas")
        Integer cuotas,

        @NotNull(message = "Elige el destino del crédito")
        Destino destino,

        boolean autorizaEvaluacion,
        boolean autorizaEnvio

) {

    public Dinero dinero() {
        return Dinero.de(monto);
    }

    public boolean autorizaDatos() {
        return autorizaEnvio && autorizaEvaluacion;
    }
}
