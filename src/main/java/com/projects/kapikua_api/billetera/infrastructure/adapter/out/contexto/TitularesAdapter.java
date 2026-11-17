package com.projects.kapikua_api.billetera.infrastructure.adapter.out.contexto;

import com.projects.kapikua_api.billetera.application.port.out.Titulares;
import com.projects.kapikua_api.identidad.application.port.in.ConsultarIdentidadUseCase;
import com.projects.kapikua_api.shared.UsuarioId;
import org.springframework.stereotype.Component;

@Component
class TitularesAdapter implements Titulares {
    private final ConsultarIdentidadUseCase identidad;

    TitularesAdapter(ConsultarIdentidadUseCase identidad) {
        this.identidad = identidad;
    }

    @Override
    public String nombreEnmascarado(UsuarioId usuarioId) {
        return identidad.nombreEnmascarado(usuarioId);
    }

    @Override
    public String correo(UsuarioId usuarioId) {
        return identidad.porId(usuarioId).map(ConsultarIdentidadUseCase.DatosIdentidad::correo).orElseThrow();
    }
}
