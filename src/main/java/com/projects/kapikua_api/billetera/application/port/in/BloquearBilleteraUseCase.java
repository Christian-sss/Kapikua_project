package com.projects.kapikua_api.billetera.application.port.in;

import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Result;

/** Para Cumplimiento (CU-14, 4a): bloquear una billetera al escalar una alerta. */
public interface BloquearBilleteraUseCase {

    /** Corre dentro de la transacción del llamador, para que la revisión y el bloqueo se guarden juntos. */
    Result<Void, ErrorBilletera> bloquearPorCumplimiento(Long billeteraId, String motivo);
}
