package com.projects.kapikua_api.shared.evento;

import com.projects.kapikua_api.shared.Dinero;

import java.time.OffsetDateTime;

public record TransaccionRegistrada(Long transaccionId,
                                    String tipo,
                                    Dinero monto,
                                    Long billeteraOrigenId,
                                    Long billeteraDestinoId,
                                    OffsetDateTime fecha) {}
