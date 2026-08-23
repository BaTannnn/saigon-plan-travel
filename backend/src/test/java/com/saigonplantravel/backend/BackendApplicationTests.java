package com.saigonplantravel.backend;

import com.saigonplantravel.backend.testsupport.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
class BackendApplicationTests {

    @Container
    static final PostgreSQLContainer postgres = PostgresIntegrationTestSupport.newContainer();

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresIntegrationTestSupport.registerCommonProperties(registry, postgres);
    }

    @Test
    void contextLoadsWithFlywayAndHibernateValidation() {}
}
