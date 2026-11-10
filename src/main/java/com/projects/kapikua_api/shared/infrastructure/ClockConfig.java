package com.projects.kapikua_api.shared.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class ClockConfig {
    @Bean
    Clock clock() {
        return Clock.system(HoraLocal.ZONA);
    }
}
