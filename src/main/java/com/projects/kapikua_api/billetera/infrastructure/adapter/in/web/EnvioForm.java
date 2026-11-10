package com.projects.kapikua_api.billetera.infrastructure.adapter.in.web;

import com.projects.kapikua_api.shared.Dinero;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class EnvioForm {
    private static final String UUID_REGEX = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

    @NotBlank(message = "Ingresa el número de celular")
    @Pattern(regexp = "^9[0-9]{8}$", message = "El celular tiene 9 dígitos y empieza con 9")
    private String celular;

    @NotNull(message = "Ingresa el monto")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero")
    @Digits(integer = 7, fraction = 2, message = "Usa hasta 2 decimales")
    private BigDecimal monto;

    @Pattern(regexp = UUID_REGEX, message = "Vuelve a empezar la operación")
    private String referencia = UUID.randomUUID().toString();

    private String codigo;

    Dinero dinero() {
        return Dinero.de(monto);
    }

    void nuevaReferencia() {
        referencia = UUID.randomUUID().toString();
    }
}
