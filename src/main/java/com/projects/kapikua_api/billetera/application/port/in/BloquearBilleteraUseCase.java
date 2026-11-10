package com.projects.kapikua_api.billetera.application.port.in;

import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Result;

public interface BloquearBilleteraUseCase {
    Result<Void, ErrorBilletera> bloquearPorCumplimiento(Long billeteraId, String motivo);
}
