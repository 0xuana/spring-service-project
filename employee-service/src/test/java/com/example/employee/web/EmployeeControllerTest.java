package com.example.employee.web;

import com.example.employee.dto.EmployeeDTO;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SpringBootTest annotation
 *  it is used to test the entire spring boot application
 *  which means spring boot need to load all beans you defined in your application
 *  from application context.
 *
 *  key feature of spring boot test annotation
 *  1: full application context: it loads the full spring boot application context,
 *      including configuration, repo, service, and all beans
 *  2: integration testing, it is ideal for integration tests where you want to
 *  test the interaction between components(controller, service, db connection, repo. etc....)
 *
 *  webmvctest annotation
 *   it is used and designed for testing spring mvc controllers
 *   it focuses only one the web layer
 *
 *
 *   what is @Retention()? it defines how long
 *    your annotation should be kept in the java lifecycle
 *      RetentionPolicy.RUNTIME -> your annotation can be found at runtime
 *      RetentionPolicy.Source -> your annotation only can be found at compile time
 *
 *   @BeforeEach // this comes from junit 5
 *     public void setup(){
 *         System.out.println("this is before each annotation ");
 *     }
 *     @BeforeAll
 *     @AfterAll
 *
 * this is key features from junit 5
 *
 *  @ParameterizedTest(name = "create({0}) → duplicate? {1}")
 *  @CsvSource({
 *             "dina@example.com, false",
 *             "alice@example.com, true"
 *     })
 *
 *    why use it?
 *    avoiding code duplication: if you need to run the same test function with multiple inputs
 *    this annotation allows you to do that without writing separate test methods for each input!!
 *
 *    csvsource() -> csv formatted data with multiple parames
 *    valueSource() -> provide single type of data (string, integer) to the test method
 *    enumsource()
 *    methodsource()
 *    ...
 *
 * @Nested annotation
 * this annotation to define nested test classes inside a test class.
 * this annotation helps you organize and group all same test cases.
 *
 *
 *
 * grey box testing:
 * tester like you has partial knowledge of the internal code.
 *
 * TDD -> test driven development
 * writes tests code first, then code to pass them
 * like red(fail) -> green( pass) -> refactor
 *
 * you are first time to build your project:
 *
 * stress testing
 * test system under extreme load beyond normal limits
 *
 * Spike testing
 * test your system where load is suddenly increased/ decreased.
 *
 *
 * Smoke testing
 *  it is used for testing your system and ensures critical functionally works
 *
 *
 *  summary of steps from testing to production
 *  1: plan and define requirements
 *  2: write code (feature development)
 *  3: write unit test cases(test individual components)
 *  4: write integration test( test interaction with other service)
 *  5: run tests locally
 *  6: pass all test cases and put code to git repo
 *  7: ci  -> continuous integration (run tests in ci pipeline after pushing code)
 *  8: code review (peer review for code quality)
 *  9: deploy to staging(QA)
 *  10: performance and security testing (optional but recommended)
 *  11: deploy to production
 *  12: monitoring all the time....
 */

@WebMvcTest(
    controllers = EmployeeController.class,
    properties = {
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.config.import="
    }
)
@DisplayName("EmployeeController (WebMvc slice)")
public class EmployeeControllerTest {
    @Autowired MockMvc mvc;
    @Autowired
    ObjectMapper om;

    @MockBean
    EmployeeService service;


    @Nested
    class List_and_Get {
        @Test
        void list_returns_200_and_empty_array_when_no_employees() throws Exception {
            when(service.getAll()).thenReturn(List.of());

            mvc.perform(get("/api/v1/employees"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        void list_returns_200_and_array() throws Exception {
            when(service.getAll()).thenReturn(List.of(
                    EmployeeDTO.builder().id(1L).firstName("Alice").lastName("Nguyen").email("alice@example.com").departmentId(10L).build(),
                    EmployeeDTO.builder().id(2L).firstName("Bob").lastName("Smith").email("bob@example.com").departmentId(20L).build()
            ));
            mvc.perform(get("/api/v1/employees"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].email").value("alice@example.com"))
                    .andExpect(jsonPath("$[1].email").value("bob@example.com"));
        }

        @Test
        void getById_returns_200_and_employee() throws Exception {
            var employee = EmployeeDTO.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .departmentId(5L)
                .build();

            when(service.getById(1L)).thenReturn(employee);

            mvc.perform(get("/api/v1/employees/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.email").value("john@example.com"))
                    .andExpect(jsonPath("$.departmentId").value(5L));
        }

        @Test
        void getById_returns_404_when_employee_not_found() throws Exception {
            when(service.getById(999L)).thenThrow(new EmployeeNotFoundException(999L));

            mvc.perform(get("/api/v1/employees/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class Create_validation {
        @Test
        void create_missing_email_returns_400() throws Exception {
            var body = EmployeeDTO.builder().firstName("No").lastName("Email").build();
            mvc.perform(post("/api/v1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void create_missing_firstName_returns_400() throws Exception {
            var body = EmployeeDTO.builder().lastName("Last").email("test@example.com").build();
            mvc.perform(post("/api/v1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void create_missing_lastName_returns_400() throws Exception {
            var body = EmployeeDTO.builder().firstName("First").email("test@example.com").build();
            mvc.perform(post("/api/v1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void create_invalid_email_format_returns_400() throws Exception {
            var body = EmployeeDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("invalid-email")
                .build();
            mvc.perform(post("/api/v1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void create_valid_returns_201() throws Exception {
            var req = EmployeeDTO.builder()
                .firstName("Dina")
                .lastName("Khan")
                .email("dina@example.com")
                .departmentId(1L)
                .build();
            var res = EmployeeDTO.builder()
                .id(10L)
                .firstName("Dina")
                .lastName("Khan")
                .email("dina@example.com")
                .departmentId(1L)
                .build();
            when(service.create(any(EmployeeDTO.class))).thenReturn(res);

            mvc.perform(post("/api/v1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(10L))
                    .andExpect(jsonPath("$.firstName").value("Dina"))
                    .andExpect(jsonPath("$.lastName").value("Khan"))
                    .andExpect(jsonPath("$.email").value("dina@example.com"))
                    .andExpect(jsonPath("$.departmentId").value(1L));
        }

        @Test
        void create_duplicate_email_returns_409() throws Exception {
            var body = EmployeeDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("existing@example.com")
                .build();

            when(service.create(any(EmployeeDTO.class)))
                .thenThrow(new DuplicateEmailException("existing@example.com"));

            mvc.perform(post("/api/v1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(body)))
                    .andExpect(status().isConflict());
        }

        @Test
        void create_with_empty_request_body_returns_400() throws Exception {
            mvc.perform(post("/api/v1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void create_with_malformed_json_returns_400() throws Exception {
            mvc.perform(post("/api/v1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json"))
                    .andExpect(status().isBadRequest());
        }
    }
}
