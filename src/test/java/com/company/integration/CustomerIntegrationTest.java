package com.company.integration;

import com.company.dto.CustomerRequest;
import com.company.dto.CustomerResponse;
import com.company.dto.LoginRequest;
import com.company.dto.RegisterRequest;
import com.company.model.Customer;
import com.company.repository.CustomerRepository;
import com.company.repository.UserRepository;
import com.company.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CustomerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private EmailService emailService;

    // Shared across ordered tests. @TestInstance(PER_CLASS) keeps one
    // instance for the whole class, so these fields survive between tests.
    private Long johnCustomerId;

    private String getToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    // ===================== SCENARIO 1: CUSTOMER REGISTERS, LOGS IN, CREATES, SEARCHES =====================

    @Test
    @Order(1)
    void customerFlow_RegisterLoginCreateSearch() throws Exception {
        // 1. Register the customer's user account
        RegisterRequest userRequest = new RegisterRequest();
        userRequest.setUsername("testuser");
        userRequest.setPassword("password");
        userRequest.setConfirmPassword("password");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated());

        // 2. Log in
        String userToken = getToken("testuser", "password");

        // 3. Create the customer profile
        CustomerRequest createRequest = new CustomerRequest();
        createRequest.setName("John Doe");
        createRequest.setEmail("john@test.com");
        createRequest.setPhone("1234567890");

        String createResponse = mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        CustomerResponse created = objectMapper.readValue(createResponse, CustomerResponse.class);
        johnCustomerId = created.getId();

        // 4. Search for it (GET by id)
        mockMvc.perform(get("/api/v1/customers/" + johnCustomerId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@test.com"));

        // 5. Verify in database
        Optional<Customer> saved = customerRepository.findByEmail("john@test.com");
        assertThat(saved).isPresent();
        assertThat(saved.get().getName()).isEqualTo("John Doe");
    }

    // ===================== SCENARIO 2: SECOND CUSTOMER REGISTERS, LOGS IN, TRIES SAME EMAIL =====================

    @Test
    @Order(2)
    void secondCustomerFlow_RegisterLoginCreateSameEmail_ReturnsConflict() throws Exception {
        // 1. Register a different user account
        RegisterRequest secondUserRequest = new RegisterRequest();
        secondUserRequest.setUsername("seconduser");
        secondUserRequest.setPassword("password");
        secondUserRequest.setConfirmPassword("password");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondUserRequest)))
                .andExpect(status().isCreated());

        // 2. Log in
        String secondUserToken = getToken("seconduser", "password");

        // 3. Try to create a profile using the same email as Scenario 1's customer
        CustomerRequest duplicateRequest = new CustomerRequest();
        duplicateRequest.setName("Jane Doe");
        duplicateRequest.setEmail("john@test.com");
        duplicateRequest.setPhone("0987654321");

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + secondUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict());
    }

    // ===================== SCENARIO 3: ADMIN REGISTERS, LOGS IN, SEARCHES, DELETES =====================

    @Test
    @Order(3)
    void adminFlow_RegisterLoginSearchDelete() throws Exception {
        // 1. Register the admin
        RegisterRequest adminRequest = new RegisterRequest();
        adminRequest.setUsername("admin");
        adminRequest.setPassword("password");
        adminRequest.setConfirmPassword("password");
        adminRequest.setRole("ADMIN");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRequest)))
                .andExpect(status().isCreated());

        // 2. Log in
        String adminToken = getToken("admin", "password");

        // 3. Search for the customer created in Scenario 1
        mockMvc.perform(get("/api/v1/customers/" + johnCustomerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"));

        // 4. Delete it
        mockMvc.perform(delete("/api/v1/customers/" + johnCustomerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // 5. Verify in database
        Optional<Customer> deleted = customerRepository.findById(johnCustomerId);
        assertThat(deleted).isEmpty();
    }
}