package com.projects.kapikua_api.credito.application.port.in;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.credito.domain.service.CalculadoraFrancesa.CuotaProgramada;
import com.projects.kapikua_api.credito.domain.model.CondicionesCredito;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** CU-10 Aceptar oferta y recibir desembolso (RF-CR-04, RF-CR-05) y el reintento ante la entidad aliada. */
public interface AceptarOfertaUseCase {

    /** CU-08, 9b: si la entidad aliada no respondió, el cliente puede volver a enviarla. */
    Result<Void, ErrorCredito> enviarAEntidad(UsuarioId usuarioId, Long ofertaId);

    /** La hoja resumen que el cliente revisa antes de aceptar (N6); genera el código de un solo uso. */
    Result<HojaResumen, ErrorCredito> hojaResumen(UsuarioId usuarioId, Long ofertaId);

    /** @return id del préstamo creado y desembolsado */
    Result<Long, ErrorCredito> aceptar(UsuarioId usuarioId, Long ofertaId, String codigo, String dispositivo,
                                       boolean autorizaCargo);

    record HojaResumen(Long ofertaId, String version, CondicionesCredito condiciones, Dinero interesTotal,
                       Dinero totalAPagar, BigDecimal tasaMoratoriaMaxima, int porcentajeRetencion, String aval,
                       LocalDate vigenteHasta, List<CuotaProgramada> cronograma, String codigoMostrado) {}
}
