package com.projects.kapikua_api.billetera.application.port.out;

import com.projects.kapikua_api.billetera.domain.error.ErrorBilletera;
import com.projects.kapikua_api.billetera.domain.model.Topes;
import com.projects.kapikua_api.shared.Result;

import java.time.LocalDate;

/** Topes vigentes de la SBS; los lee de Cumplimiento (RNF-12). */
public interface ParametrosRegulatorios {

    Result<Topes, ErrorBilletera> topesVigentes(LocalDate fecha);
}
