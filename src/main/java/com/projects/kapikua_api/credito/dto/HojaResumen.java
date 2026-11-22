package com.projects.kapikua_api.credito.dto;

import com.projects.kapikua_api.credito.model.CalculadoraFrancesa.CuotaProgramada;
import com.projects.kapikua_api.credito.model.CondicionesCredito;
import com.projects.kapikua_api.shared.Dinero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Hoja resumen que el cliente revisa antes de aceptar la oferta, con el código de un solo uso. */
public record HojaResumen(Long ofertaId, String version, CondicionesCredito condiciones, Dinero interesTotal,
                          Dinero totalAPagar, BigDecimal tasaMoratoriaMaxima, int porcentajeRetencion, String aval,
                          LocalDate vigenteHasta, List<CuotaProgramada> cronograma, String codigoMostrado) {
}
