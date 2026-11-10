package com.projects.kapikua_api.cumplimiento.application.port.out;

import com.projects.kapikua_api.cumplimiento.domain.model.Parametro;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ParametroRepositoryPort {

    Optional<Parametro> vigente(String codigo, LocalDate fecha);

    List<Parametro> vigentes(LocalDate fecha);
}
