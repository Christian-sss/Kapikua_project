package com.projects.kapikua_api.credito.application.port.out;

import com.projects.kapikua_api.shared.UsuarioId;

import java.util.Optional;

/** Identidad: consentimientos (RN-07) y datos del cliente para la entidad aliada y las pantallas. */
public interface ClientesCredito {

    /** CU-08, paso 2: evaluar con los datos de la billetera y enviarlos a la entidad aliada. */
    void otorgarConsentimientosDeSolicitud(UsuarioId usuarioId);

    /** CU-09, paso 5: el aval autoriza el uso de sus datos y la retención de sus ventas. */
    void otorgarConsentimientoDeGarantia(UsuarioId avalId);

    /** DNI completo, solo para la solicitud a la entidad aliada. */
    Optional<String> dni(UsuarioId usuarioId);

    /** "Ana Q.": nombre parcialmente oculto para el aval y el deudor. */
    String nombreEnmascarado(UsuarioId usuarioId);
}
