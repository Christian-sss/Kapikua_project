package com.projects.kapikua_api.billetera.application.port.out;

import com.projects.kapikua_api.billetera.domain.model.Billetera;
import com.projects.kapikua_api.billetera.domain.model.Celular;
import com.projects.kapikua_api.shared.UsuarioId;

import java.util.Optional;

public interface BilleteraRepositoryPort {

    Optional<Billetera> porUsuario(UsuarioId usuarioId);

    Optional<Billetera> porCelular(Celular celular);

    Optional<Billetera> porId(Long billeteraId);

    boolean existeCelular(Celular celular);

    /** Con bloqueo optimista: si otra transacción la cambió antes, lanza OptimisticLockingFailureException. */
    Billetera guardar(Billetera billetera);
}
