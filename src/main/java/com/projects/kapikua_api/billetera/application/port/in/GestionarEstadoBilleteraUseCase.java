package com.projects.kapikua_api.billetera.application.port.in;

import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Result;

public interface GestionarEstadoBilleteraUseCase {
    Result<Void, ErrorBilletera> bloquearPorCumplimiento(Long billeteraId, String motivo);

    Result<Void, ErrorBilletera> retener(Long billeteraId, String motivo);

    Result<Void, ErrorBilletera> reactivar(Long billeteraId);
}
