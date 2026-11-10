package com.projects.kapikua_api.billetera.application.service;

import com.projects.kapikua_api.billetera.application.port.in.AbrirBilleteraUseCase;
import com.projects.kapikua_api.billetera.application.port.out.BilleteraRepositoryPort;
import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.billetera.domain.model.Billetera;
import com.projects.kapikua_api.billetera.domain.model.Celular;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;

/** Corre dentro de la transacción del registro: si falla, el registro completo se revierte. */
@Service
public class AbrirBilleteraService implements AbrirBilleteraUseCase {

    private final BilleteraRepositoryPort billeteras;
    private final Clock clock;

    public AbrirBilleteraService(BilleteraRepositoryPort billeteras, Clock clock) {
        this.billeteras = billeteras;
        this.clock = clock;
    }

    @Override
    public Result<Long, ErrorBilletera> abrir(UsuarioId usuarioId, String celular, String nombreNegocio) {
        var numero = Celular.de(celular);
        if (numero.isEmpty()) {
            return Result.err(new ErrorBilletera.CelularInvalido());
        }
        if (billeteras.porUsuario(usuarioId).isPresent() || billeteras.existeCelular(numero.get())) {
            throw new IllegalStateException("El usuario o el celular ya tienen billetera");
        }
        var billetera = Billetera.abrir(usuarioId, numero.get(), nombreNegocio, OffsetDateTime.now(clock));
        return Result.ok(billeteras.guardar(billetera).getId());
    }
}
