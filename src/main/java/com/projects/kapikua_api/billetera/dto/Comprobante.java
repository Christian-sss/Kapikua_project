package com.projects.kapikua_api.billetera.dto;

import com.projects.kapikua_api.billetera.model.TipoTransaccion;
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
