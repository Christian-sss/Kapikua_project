package com.projects.kapikua_api.billetera.application.port.in;

import com.projects.kapikua_api.billetera.domain.model.TipoTransaccion;
import com.projects.kapikua_api.shared.Dinero;

import java.time.OffsetDateTime;

public record Comprobante(Long transaccionId,
      TipoTransaccion tipo,
      Dinero monto,
      String referencia,
      String contraparte,
      OffsetDateTime fecha,
      Dinero saldoPosterior) {
}
