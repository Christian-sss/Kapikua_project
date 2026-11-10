package com.projects.kapikua_api.shared.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Activa las tareas programadas: el latido SSE y, más adelante, el cobro diario (CU-13). */
@Configuration
@EnableScheduling
class ProgramacionConfig {
}
