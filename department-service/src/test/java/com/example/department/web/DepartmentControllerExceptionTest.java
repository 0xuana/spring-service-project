package com.example.department.web;

import com.example.department.exception.DepartmentNotFoundException;
import com.example.department.exception.DuplicateDepartmentException;
import com.example.department.exception.GlobalExceptionHandler;
import com.example.department.service.DepartmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DepartmentController.class)
@Import(GlobalExceptionHandler.class)
class DepartmentControllerExceptionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DepartmentService departmentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getById_NotFound_Returns404WithProblemDetail() throws Exception {
        when(departmentService.getByIdAsDTO(anyLong()))
            .thenThrow(new DepartmentNotFoundException(999L));

        mockMvc.perform(get("/api/v1/departments/999"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("https://api.company.com/problems/not-found"))
            .andExpect(jsonPath("$.title").value("Resource Not Found"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.detail").value("Department not found with id: 999"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.traceId").exists())
            .andExpect(jsonPath("$.instance").exists());
    }

    @Test
    void create_DuplicateName_Returns409WithProblemDetail() throws Exception {
        String duplicateName = "Engineering";
        when(departmentService.createFromDTO(any()))
            .thenThrow(new DuplicateDepartmentException(duplicateName));

        String requestBody = """
            {
                "code": "ENG",
                "name": "Engineering",
                "description": "Software development department"
            }
            """;

        mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isConflict())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("https://api.company.com/problems/duplicate-resource"))
            .andExpect(jsonPath("$.title").value("Duplicate Resource"))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.detail").value("Department with name already exists: " + duplicateName))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.traceId").exists())
            .andExpect(jsonPath("$.instance").exists());
    }

    @Test
    void create_ValidationFailure_Returns400WithProblemDetail() throws Exception {
        String requestBody = """
            {
                "code": "ENG",
                "name": "",
                "description": "This description is way too long and exceeds the maximum allowed length for a department description which should be limited to 2000 characters but this one goes on and on and on and provides way too much information that nobody really needs to read because it's just a test case to verify that validation is working properly and that we get the expected error response when the field length exceeds the maximum allowed value that we have configured in our validation annotations on the Department entity class which is used to ensure data integrity and prevent bad data from being stored in our database system that could potentially cause issues down the road when we try to retrieve or display this information to users who are expecting reasonable and concise descriptions of the various departments in our organization and not these incredibly long run-on sentences that don't really provide any useful information but instead just serve to test our validation logic and error handling capabilities which are very important for maintaining a robust and reliable application that can handle all sorts of edge cases and unexpected input from users who might not always follow the expected patterns or provide data in the exact format that we're expecting them to use when interacting with our API endpoints and other system components that depend on having clean, well-formed data to work with effectively and efficiently without encountering errors or unexpected behavior that could negatively impact the user experience or cause system instability or other problems that we definitely want to avoid at all costs because they can be very difficult and time-consuming to debug and fix especially in production environments where downtime and errors can have serious business implications and consequences for our organization and its stakeholders who depend on our systems to be reliable and available when they need them most which is why we invest so much time and effort into testing and validation and error handling and all the other aspects of software development that help ensure our applications are robust and reliable and meet the high standards that our users and customers expect from us as a technology company that takes pride in delivering high-quality software solutions that solve real-world problems and provide value to the people who use them on a daily basis for their work and personal needs and requirements which can vary widely depending on their specific use cases and workflows and other factors that we need to consider when designing and implementing our software systems and user interfaces and other components that make up the overall user experience that we're trying to create and optimize for maximum effectiveness and usability"
            }
            """;

        mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("https://api.company.com/problems/validation-error"))
            .andExpect(jsonPath("$.title").value("Validation Error"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("Validation failed for one or more fields"))
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errors[*].field").exists())
            .andExpect(jsonPath("$.errors[*].message").exists())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.traceId").exists())
            .andExpect(jsonPath("$.instance").exists());
    }

    @Test
    void internalError_Returns500WithProblemDetail() throws Exception {
        when(departmentService.getByIdAsDTO(anyLong()))
            .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(get("/api/v1/departments/1"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("https://api.company.com/problems/internal-error"))
            .andExpect(jsonPath("$.title").value("Internal Server Error"))
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.detail").value("An unexpected error occurred. Please try again later."))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.traceId").exists())
            .andExpect(jsonPath("$.instance").exists());
    }
}