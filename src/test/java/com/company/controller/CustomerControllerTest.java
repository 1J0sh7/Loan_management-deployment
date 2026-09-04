package com.company.controller;

import com.company.dto.LoginRequest;
import com.company.dto.RegisterRequest;
import com.company.dto.CustomerRequest;
import com.company.dto.CustomerResponse;
import com.company.dto.PageResponse;
import com.company.exception.DuplicateResourceException;
import com.company.exception.NotFoundException;
import com.company.exception.ValidationException;
import com.company.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerService customerService;

    private String userToken;
    private String adminToken;
    private String otherUserToken;

    @BeforeEach
    void setUp() throws Exception {
        // Step 1: Register a regular user
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("password");
        registerRequest.setConfirmPassword("password");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Step 2: Register an ADMIN user
        RegisterRequest adminRequest = new RegisterRequest();
        adminRequest.setUsername("admin");
        adminRequest.setPassword("password");
        adminRequest.setConfirmPassword("password");
        adminRequest.setRole("ADMIN");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRequest)))
                .andExpect(status().isCreated());

        // Step 3: Register another regular user (for ownership tests)
        RegisterRequest otherRequest = new RegisterRequest();
        otherRequest.setUsername("otheruser");
        otherRequest.setPassword("password");
        otherRequest.setConfirmPassword("password");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherRequest)))
                .andExpect(status().isCreated());

        // Step 4: Login each user to get their token
        userToken = getToken("testuser", "password");
        adminToken = getToken("admin", "password");
        otherUserToken = getToken("otheruser", "password");
    }

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

    // ========================= GET ALL =========================





    @Test
    void getAllWithAddresses_ReturnsOk() throws Exception {
        CustomerResponse response = new CustomerResponse(1L, "John Doe", "john@test.com", "1234567890");
        when(customerService.getAllCustomersWithAddresses()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/customers/with-addresses")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("John Doe"));
    }

    // ========================= CREATE =========================

    @Test
    void createCustomer_ValidRequest_ReturnsCreated() throws Exception {
        CustomerRequest request = new CustomerRequest();
        request.setName("John Doe");
        request.setEmail("john@test.com");
        request.setPhone("1234567890");

        CustomerResponse response = new CustomerResponse(1L, "John Doe", "john@test.com", "1234567890");

        when(customerService.createCustomer(any(CustomerRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    void createCustomer_BlankName_ReturnsBadRequest() throws Exception {
        CustomerRequest request = new CustomerRequest();
        request.setName("");
        request.setEmail("john@test.com");
        request.setPhone("1234567890");

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCustomer_InvalidEmail_ReturnsBadRequest() throws Exception {
        CustomerRequest request = new CustomerRequest();
        request.setName("John");
        request.setEmail("not-an-email");
        request.setPhone("1234567890");

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCustomer_DuplicateEmail_ReturnsConflict() throws Exception {
        CustomerRequest request = new CustomerRequest();
        request.setName("John");
        request.setEmail("john@test.com");
        request.setPhone("1234567890");

        when(customerService.createCustomer(any(CustomerRequest.class)))
                .thenThrow(new DuplicateResourceException("Customer with that email already exists"));

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void createCustomer_UserAlreadyHasProfile_ReturnsBadRequest() throws Exception {
        CustomerRequest request = new CustomerRequest();
        request.setName("John");
        request.setEmail("john@test.com");
        request.setPhone("1234567890");

        when(customerService.createCustomer(any(CustomerRequest.class)))
                .thenThrow(new ValidationException("You already have a customer profile."));

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCustomer_Admin_ReturnsForbidden() throws Exception {
        CustomerRequest request = new CustomerRequest();
        request.setName("John");
        request.setEmail("john@test.com");
        request.setPhone("1234567890");

        when(customerService.createCustomer(any(CustomerRequest.class)))
                .thenThrow(new AccessDeniedException("ADMIN cannot create customer profiles."));

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ========================= GET BY ID =========================

    @Test
    void getCustomerById_Exists_ReturnsOk() throws Exception {
        CustomerResponse response = new CustomerResponse(1L, "John Doe", "john@test.com", "1234567890");
        when(customerService.getCustomerById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/customers/1")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    void getCustomerById_NotFound_ReturnsNotFound() throws Exception {
        when(customerService.getCustomerById(999L)).thenThrow(new NotFoundException("Customer not found"));

        mockMvc.perform(get("/api/v1/customers/999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCustomerById_NotOwner_ReturnsForbidden() throws Exception {
        when(customerService.getCustomerById(1L))
                .thenThrow(new AccessDeniedException("You can only view your own profile"));

        mockMvc.perform(get("/api/v1/customers/1")
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isForbidden());
    }

    // ========================= UPDATE =========================

    @Test
    void updateCustomer_ValidRequest_ReturnsOk() throws Exception {
        CustomerRequest request = new CustomerRequest();
        request.setName("John Updated");
        request.setEmail("john.updated@test.com");
        request.setPhone("0987654321");

        CustomerResponse response = new CustomerResponse(1L, "John Updated", "john.updated@test.com", "0987654321");

        when(customerService.updateCustomer(eq(1L), any(CustomerRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/customers/1")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Updated"));
    }

    @Test
    void updateCustomer_NotFound_ReturnsNotFound() throws Exception {
        CustomerRequest request = new CustomerRequest();
        request.setName("John");
        request.setEmail("john@test.com");
        request.setPhone("1234567890");

        when(customerService.updateCustomer(eq(999L), any(CustomerRequest.class)))
                .thenThrow(new NotFoundException("Customer not found"));

        mockMvc.perform(put("/api/v1/customers/999")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCustomer_NotOwner_ReturnsForbidden() throws Exception {
        CustomerRequest request = new CustomerRequest();
        request.setName("John");
        request.setEmail("john@test.com");
        request.setPhone("1234567890");

        when(customerService.updateCustomer(eq(1L), any(CustomerRequest.class)))
                .thenThrow(new AccessDeniedException("You can only update your own profile"));

        mockMvc.perform(put("/api/v1/customers/1")
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateCustomer_DuplicateEmail_ReturnsConflict() throws Exception {
        CustomerRequest request = new CustomerRequest();
        request.setName("John");
        request.setEmail("taken@test.com");
        request.setPhone("1234567890");

        when(customerService.updateCustomer(eq(1L), any(CustomerRequest.class)))
                .thenThrow(new DuplicateResourceException("email already exists"));

        mockMvc.perform(put("/api/v1/customers/1")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateCustomer_BlankName_ReturnsBadRequest() throws Exception {
        CustomerRequest request = new CustomerRequest();
        request.setName("");
        request.setEmail("john@test.com");
        request.setPhone("1234567890");

        mockMvc.perform(put("/api/v1/customers/1")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ========================= PATCH =========================

    @Test
    void patchCustomer_ValidRequest_ReturnsOk() throws Exception {
        CustomerResponse response = new CustomerResponse(1L, "John Doe", "john@test.com", "9998887777");
        String patchBody = "{\"phone\":\"9998887777\"}";

        when(customerService.patchCustomer(eq(1L), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/customers/1")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("9998887777"));
    }

    @Test
    void patchCustomer_InvalidEmail_ReturnsBadRequest() throws Exception {
        String patchBody = "{\"email\":\"not-an-email\"}";

        when(customerService.patchCustomer(eq(1L), any()))
                .thenThrow(new ValidationException("Email must be valid"));

        mockMvc.perform(patch("/api/v1/customers/1")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchCustomer_NotFound_ReturnsNotFound() throws Exception {
        String patchBody = "{\"phone\":\"9998887777\"}";

        when(customerService.patchCustomer(eq(999L), any()))
                .thenThrow(new NotFoundException("Customer not found"));

        mockMvc.perform(patch("/api/v1/customers/999")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchCustomer_NotOwner_ReturnsForbidden() throws Exception {
        String patchBody = "{\"phone\":\"9998887777\"}";

        when(customerService.patchCustomer(eq(1L), any()))
                .thenThrow(new AccessDeniedException("You can only update your own profile"));

        mockMvc.perform(patch("/api/v1/customers/1")
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void patchCustomer_DuplicatePhone_ReturnsConflict() throws Exception {
        String patchBody = "{\"phone\":\"9998887777\"}";

        when(customerService.patchCustomer(eq(1L), any()))
                .thenThrow(new DuplicateResourceException("phone already exists"));

        mockMvc.perform(patch("/api/v1/customers/1")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchBody))
                .andExpect(status().isConflict());
    }

    // ========================= DELETE =========================

    @Test
    void deleteCustomer_Admin_ReturnsNoContent() throws Exception {
        when(customerService.deleteCustomer(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/customers/1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCustomer_User_ReturnsForbidden() throws Exception {
        when(customerService.deleteCustomer(1L))
                .thenThrow(new AccessDeniedException("Only ADMIN can delete"));

        mockMvc.perform(delete("/api/v1/customers/1")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteCustomer_NotFound_ReturnsNotFound() throws Exception {
        when(customerService.deleteCustomer(999L))
                .thenThrow(new NotFoundException("Customer not found"));

        mockMvc.perform(delete("/api/v1/customers/999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}