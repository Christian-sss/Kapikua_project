package com.projects.kapikua_api.billetera.application.service;

import com.projects.kapikua_api.billetera.application.port.in.BloquearBilleteraUseCase;
import com.projects.kapikua_api.billetera.application.port.out.BilleteraRepositoryPort;
import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.shared.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BloquearBilleteraService implements BloquearBilleteraUseCase {

    private final BilleteraRepositoryPort billeteras;

    public BloquearBilleteraService(BilleteraRepositoryPort billeteras) {
        this.billeteras = billeteras;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Result<Void, ErrorBilletera> bloquearPorCumplimiento(Long billeteraId, String motivo) {
        var billetera = billeteras.porId(billeteraId);
        if (billetera.isEmpty()) return Result.err(new ErrorBilletera.BilleteraNoEncontrada());
        if (!billetera.get().estaActiva()) return Result.ok();
        billetera.get().bloquear(motivo);
        billeteras.guardar(billetera.get());
        return Result.ok();
    }
}
