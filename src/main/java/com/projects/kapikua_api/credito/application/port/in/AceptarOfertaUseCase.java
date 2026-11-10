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

public interface AceptarOfertaUseCase {
    Result<Void, ErrorCredito> enviarAEntidad(UsuarioId usuarioId, Long ofertaId);

    Result<HojaResumen, ErrorCredito> hojaResumen(UsuarioId usuarioId, Long ofertaId);

    Result<Long, ErrorCredito> aceptar(UsuarioId usuarioId, Long ofertaId, String codigo, String dispositivo,
                                       boolean autorizaCargo);

    record HojaResumen(Long ofertaId, String version, CondicionesCredito condiciones, Dinero interesTotal,
                       Dinero totalAPagar, BigDecimal tasaMoratoriaMaxima, int porcentajeRetencion, String aval,
                       LocalDate vigenteHasta, List<CuotaProgramada> cronograma, String codigoMostrado) {}
}
