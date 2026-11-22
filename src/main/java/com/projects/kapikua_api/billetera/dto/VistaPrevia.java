package com.projects.kapikua_api.billetera.dto;

import com.projects.kapikua_api.shared.Dinero;

public record VistaPrevia(String destinatario, String celularEnmascarado, Dinero monto, String referencia,
                          String codigoMostrado) {
}
