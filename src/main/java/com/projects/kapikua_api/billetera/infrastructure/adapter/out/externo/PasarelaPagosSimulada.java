package com.projects.kapikua_api.billetera.infrastructure.adapter.out.externo;

import com.projects.kapikua_api.billetera.application.port.out.PasarelaPagos;
import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PROVISIONAL: pasarela simulada hasta configurar el adaptador de Culqi con sus llaves de integración.
 * Reglas para probar cada caso:
 * <ul>
 *   <li>token "tok_rechazado": tarjeta rechazada (CU-04, 5a).</li>
 *   <li>cualquier otro token: cargo aprobado con un id "chr_sim_...".</li>
 * </ul>
 * Una misma referencia de formulario devuelve el mismo cargo, como haría la pasarela ante un reintento.
 */
@Component
class PasarelaPagosSimulada implements PasarelaPagos {

    private final Map<String, String> cargosPorReferencia = new ConcurrentHashMap<>();

    @Override
    public Result<String, ErrorBilletera> cargar(Dinero monto, String token, String correo, String referencia) {
        if (token == null || token.isBlank() || "tok_rechazado".equals(token)) {
            return Result.err(new ErrorBilletera.PagoRechazado("tarjeta sin fondos o datos inválidos."));
        }
        return Result.ok(cargosPorReferencia.computeIfAbsent(referencia,
                r -> "chr_sim_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20)));
    }
}
