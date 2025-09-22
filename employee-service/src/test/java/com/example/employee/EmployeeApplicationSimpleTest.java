package com.example.employee;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.config.import="
    }
)
@ActiveProfiles("test")
public class EmployeeApplicationSimpleTest {

    @Test
    void contextLoads() {
        // This test ensures that the Spring Boot application context can load successfully
        // without any database or external service dependencies
    }
}