package com.example.employee;

import com.example.employee.repo.EmployeeRepository;
import com.example.employee.client.DepartmentClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;


import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.config.enabled=false",
                "spring.config.import=optional:file:./config/",
                // If you restrict actuator exposure in prod, make sure health is exposed here:
                "management.endpoints.web.exposure.include=health,info"
        }
)
class EmployeeActuatorSpringBootTest {
    @Autowired
    TestRestTemplate rest;

    @MockBean EmployeeRepository repository;
    @MockBean DepartmentClient departmentClient;

    @Test
    void health_is_UP() {
        ResponseEntity<String> res = rest.getForEntity("/actuator/health", String.class);
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(res.getBody()).contains("\"status\":\"UP\"");
    }

}
