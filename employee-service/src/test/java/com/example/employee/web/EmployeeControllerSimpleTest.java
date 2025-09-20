package com.example.employee.web;

import com.example.employee.dto.EmployeeDTO;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
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

@WebMvcTest(
    controllers = EmployeeController.class,
    properties = {
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false"
    }
)
public class EmployeeControllerSimpleTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @MockBean
    EmployeeService service;

    @Test
    void getAll_returnsOk() throws Exception {
        when(service.getAll()).thenReturn(List.of());

        mvc.perform(get("/api/v1/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getById_returns404_whenNotFound() throws Exception {
        when(service.getById(999L)).thenThrow(new EmployeeNotFoundException(999L));

        mvc.perform(get("/api/v1/employees/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returnsCreated_whenValidData() throws Exception {
        var request = EmployeeDTO.builder()
            .firstName("John")
            .lastName("Doe")
            .email("john@example.com")
            .build();

        var response = EmployeeDTO.builder()
            .id(1L)
            .firstName("John")
            .lastName("Doe")
            .email("john@example.com")
            .build();

        when(service.create(any(EmployeeDTO.class))).thenReturn(response);

        mvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void create_returnsBadRequest_whenMissingFields() throws Exception {
        var request = EmployeeDTO.builder()
            .firstName("John")
            // Missing lastName and email
            .build();

        mvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}