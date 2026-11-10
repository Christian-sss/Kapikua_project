package com.projects.kapikua_api.credito.application.port.in;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.credito.domain.model.ModalidadPago;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

import java.util.List;

/** CU-11 Pagar cuota o adelantar pago (RF-CR-06, RF-CR-07), y revocar el cargo automático (RN-22). */
public interface PagarPrestamoUseCase {

    /** Lo que se pagaría: las cuotas vencidas o, si no hay, la próxima; con su interés moratorio al día. */
    Result<VistaPago, ErrorCredito> prepararPagoCuotas(UsuarioId usuarioId);

    /** @param referencia UUID del formulario: repetir el envío no cobra dos veces (RNF-07) */
    Result<Dinero, ErrorCredito> pagarCuotas(UsuarioId usuarioId, String referencia);

    Result<VistaAnticipado, ErrorCredito> prepararAnticipado(UsuarioId usuarioId);

    /** RN-27: sin penalidad. Con TOTAL se ignora {@code monto} y se cobra el capital pendiente. */
    Result<Dinero, ErrorCredito> pagarAnticipado(UsuarioId usuarioId, ModalidadPago modalidad, Dinero monto,
                                                 String referencia);

    /** RN-22: se dejan de retener ventas y lo apartado vuelve a estar disponible. */
    Result<Dinero, ErrorCredito> revocarCargo(UsuarioId usuarioId);

    /** @param deApartado cuánto sale de lo ya retenido de tus ventas */
    record VistaPago(List<Integer> cuotas, Dinero total, Dinero moratorio, Dinero deApartado, String referencia) {}

    record VistaAnticipado(Dinero capitalPendiente, int cuotasPendientes, boolean hayVencidas, String referencia) {}
}
