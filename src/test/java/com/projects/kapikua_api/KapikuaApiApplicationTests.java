package com.projects.kapikua_api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "KAPIKUA_DB_PRUEBAS", matches = "jdbc:postgresql:.+")
class KapikuaApiApplicationTests {
    @DynamicPropertySource
    static void base(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", () -> System.getenv("KAPIKUA_DB_PRUEBAS"));
    }

    @Test
    void contextLoads() {
    }

}
