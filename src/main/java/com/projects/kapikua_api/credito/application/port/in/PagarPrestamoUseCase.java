package com.projects.kapikua_api.credito.application.port.in;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.credito.domain.model.ModalidadPago;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

import java.util.List;

public interface PagarPrestamoUseCase {
    Result<VistaPago, ErrorCredito> prepararPagoCuotas(UsuarioId usuarioId);

    Result<Dinero, ErrorCredito> pagarCuotas(UsuarioId usuarioId, String referencia);

    Result<VistaAnticipado, ErrorCredito> prepararAnticipado(UsuarioId usuarioId);

    Result<Dinero, ErrorCredito> pagarAnticipado(UsuarioId usuarioId, ModalidadPago modalidad, Dinero monto,
                                                 String referencia);

    Result<Dinero, ErrorCredito> revocarCargo(UsuarioId usuarioId);

    record VistaPago(List<Integer> cuotas, Dinero total, Dinero moratorio, Dinero deApartado, String referencia) {}

    record VistaAnticipado(Dinero capitalPendiente, int cuotasPendientes, boolean hayVencidas, String referencia) {}
}
