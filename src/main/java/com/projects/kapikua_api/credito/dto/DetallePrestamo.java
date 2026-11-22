package com.projects.kapikua_api.credito.dto;

import com.projects.kapikua_api.credito.model.ConstanciaAceptacion;
import com.projects.kapikua_api.credito.model.Cuota;
import com.projects.kapikua_api.shared.Dinero;

import java.math.BigDecimal;
import java.util.List;

/** El préstamo activo del cliente con su cronograma. */
public record DetallePrestamo(Long id, String estado, String codigoCredito, Dinero monto, BigDecimal tea,
                              BigDecimal tcea, int porcentajeRetencion, Dinero capitalPendiente,
                              ConstanciaAceptacion constancia, String aval, List<Cuota> cuotas,
                              boolean cargoAutorizado, Dinero apartado, int cuotasVencidas, Dinero vencido) {
}
