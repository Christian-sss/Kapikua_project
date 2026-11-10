package com.projects.kapikua_api.shared.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** La hora se inyecta para poder probar plazos (48 h, 7 días, 15 días) sin esperar. */
@Configuration
class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.system(HoraLocal.ZONA);
    }
}
