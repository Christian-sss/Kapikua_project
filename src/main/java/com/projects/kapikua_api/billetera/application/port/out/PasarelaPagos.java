package com.projects.kapikua_api.billetera.application.port.out;

import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;

public interface PasarelaPagos {
    Result<String, ErrorBilletera> cargar(Dinero monto, String token, String correo, String referencia);
}
