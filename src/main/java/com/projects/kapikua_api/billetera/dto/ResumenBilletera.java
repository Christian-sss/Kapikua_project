package com.projects.kapikua_api.billetera.dto;

import com.projects.kapikua_api.shared.Dinero;

import java.time.LocalDate;
import java.util.List;

public record ResumenBilletera(String celular,
                               String nombreNegocio,
                               Dinero saldo,
                               Dinero saldoApartado,
                               Dinero saldoDisponible,
                               Dinero pagosUsados,
                               Dinero pagosDisponibles,
                               Dinero topePorOperacion,
                               Dinero saldoMaximo,
                               LocalDate desde,
                               LocalDate hasta,
                               List<LineaMovimiento> movimientos) {
}
