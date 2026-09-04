package com.company.service;

import com.company.dto.CustomerRequest;
import com.company.dto.CustomerResponse;
import com.company.dto.PageResponse;
import com.company.exception.DuplicateResourceException;
import com.company.exception.NotFoundException;
import com.company.exception.ValidationException;
import com.company.model.Customer;
import com.company.model.Role;
import com.company.model.User;
import com.company.repository.CustomerRepository;
import com.company.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private CustomerService customerService;

    private User testUser;
    private User adminUser;
    private Customer testCustomer;
    private Customer orphanCustomer;
    private CustomerRequest validRequest;

    // This runs before each test to set up test data
    @BeforeEach
    void setUp() {
        // Create a regular user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(Role.USER);

        // Create an admin user
        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setRole(Role.ADMIN);

        // Create a customer linked to testUser
        testCustomer = new Customer();
        testCustomer.setId(1L);
        testCustomer.setName("John Doe");
        testCustomer.setEmail("john@test.com");
        testCustomer.setPhone("1234567890");
        testCustomer.setUser(testUser);

        // Create an orphan customer (no user linked)
        orphanCustomer = new Customer();
        orphanCustomer.setId(2L);
        orphanCustomer.setName("Orphan Corp");
        orphanCustomer.setEmail("orphan@test.com");
        orphanCustomer.setPhone("5555555555");
        orphanCustomer.setUser(null);

        // A valid customer request
        validRequest = new CustomerRequest();
        validRequest.setName("John Doe");
        validRequest.setEmail("john@test.com");
        validRequest.setPhone("1234567890");

        // Set default authentication as testUser
        authenticateAs(testUser);
    }

    // Clean up after each test
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // Helper method to authenticate as a specific user
    private void authenticateAs(User user) {
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn(user.getUsername());

        String role = user.getRole() == Role.ADMIN ? "ROLE_ADMIN" : "ROLE_USER";
        lenient().doReturn(AuthorityUtils.createAuthorityList(role))
                .when(authentication).getAuthorities();

        SecurityContextHolder.getContext().setAuthentication(authentication);
        lenient().when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    }

    // Tests for createCustomer()

    @Test
    void createCustomer_Success() {
        when(customerRepository.existsByEmail(anyString())).thenReturn(false);
        when(customerRepository.existsByPhone(anyString())).thenReturn(false);
        when(customerRepository.existsByUserId(anyLong())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        CustomerResponse response = customerService.createCustomer(validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("John Doe");
        assertThat(response.getEmail()).isEqualTo("john@test.com");
        verify(customerRepository).save(any(Customer.class));
        verify(emailService).sendWelcomeEmail(anyString(), anyString());
    }

    @Test
    void createCustomer_DuplicateEmail_ThrowsException() {
        when(customerRepository.existsByUserId(anyLong())).thenReturn(false);
        when(customerRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(validRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email already exists");
    }

    @Test
    void createCustomer_DuplicatePhone_ThrowsException() {
        when(customerRepository.existsByUserId(anyLong())).thenReturn(false);
        when(customerRepository.existsByEmail(anyString())).thenReturn(false);
        when(customerRepository.existsByPhone(anyString())).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(validRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("phone already exists");
    }

    @Test
    void createCustomer_UserAlreadyHasProfile_ThrowsException() {
        when(customerRepository.existsByUserId(anyLong())).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(validRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already have a customer profile");
    }

    @Test
    void createCustomer_AdminBlocked_ThrowsException() {
        authenticateAs(adminUser);

        assertThatThrownBy(() -> customerService.createCustomer(validRequest))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ADMIN cannot create customer profiles");

        verifyNoInteractions(customerRepository);
    }

    // Tests for getCustomerById()

    @Test
    void getCustomerById_Success_Owner() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        CustomerResponse response = customerService.getCustomerById(1L);

        assertThat(response.getName()).isEqualTo("John Doe");
    }

    @Test
    void getCustomerById_NotFound_ThrowsException() {
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void getCustomerById_NotOwner_ThrowsAccessDenied() {
        User otherUser = new User();
        otherUser.setId(99L);
        otherUser.setUsername("other");
        otherUser.setRole(Role.USER);
        authenticateAs(otherUser);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        assertThatThrownBy(() -> customerService.getCustomerById(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("your own profile");
    }

    @Test
    void getCustomerById_Orphan_AdminCanView() {
        authenticateAs(adminUser);
        when(customerRepository.findById(2L)).thenReturn(Optional.of(orphanCustomer));

        CustomerResponse response = customerService.getCustomerById(2L);

        assertThat(response.getName()).isEqualTo("Orphan Corp");
    }

    @Test
    void getCustomerById_Orphan_NonAdminForbidden() {
        when(customerRepository.findById(2L)).thenReturn(Optional.of(orphanCustomer));

        assertThatThrownBy(() -> customerService.getCustomerById(2L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only ADMIN can view it");
    }

    // Tests for getAllCustomers()

    @Test
    void getAllCustomers_NoFilters_ReturnsAll() {
        Page<Customer> page = new PageImpl<>(List.of(testCustomer));
        when(customerRepository.findAll(any(Pageable.class))).thenReturn(page);

        PageResponse<CustomerResponse> result =
                customerService.getAllCustomers(0, 10, null, null, null);

        assertThat(result.getContent()).hasSize(1);
        verify(customerRepository).findAll(any(Pageable.class));
        verify(customerRepository, never())
                .findByNameContainingIgnoreCase(anyString(), any());
    }

    @Test
    void getAllCustomers_NameFilter_UsesNameQuery() {
        Page<Customer> page = new PageImpl<>(List.of(testCustomer));
        when(customerRepository.findByNameContainingIgnoreCase(eq("John"), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<CustomerResponse> result =
                customerService.getAllCustomers(0, 10, "John", null, null);

        assertThat(result.getContent()).hasSize(1);
        verify(customerRepository).findByNameContainingIgnoreCase(eq("John"), any(Pageable.class));
    }

    @Test
    void getAllCustomers_NameAndEmailFilter_UsesCombinedQuery() {
        Page<Customer> page = new PageImpl<>(List.of(testCustomer));
        when(customerRepository.findByNameContainingIgnoreCaseAndEmailContainingIgnoreCase(
                eq("John"), eq("john@test.com"), any(Pageable.class))).thenReturn(page);

        PageResponse<CustomerResponse> result =
                customerService.getAllCustomers(0, 10, "John", "john@test.com", null);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getAllCustomers_SortDescending_ParsesDirection() {
        Page<Customer> page = new PageImpl<>(List.of(testCustomer));
        when(customerRepository.findAll(any(Pageable.class))).thenReturn(page);

        customerService.getAllCustomers(0, 10, null, null, "name,desc");

        verify(customerRepository).findAll(argThat((Pageable p) ->
                p.getSort().getOrderFor("name") != null
                        && p.getSort().getOrderFor("name").isDescending()));
    }

    // Tests for updateCustomer()

    @Test
    void updateCustomer_Success() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.existsByEmail(anyString())).thenReturn(false);
        when(customerRepository.existsByPhone(anyString())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        CustomerRequest updateRequest = new CustomerRequest();
        updateRequest.setName("John Updated");
        updateRequest.setEmail("john.updated@test.com");
        updateRequest.setPhone("0987654321");

        CustomerResponse response = customerService.updateCustomer(1L, updateRequest);

        assertThat(response.getName()).isEqualTo("John Updated");
    }

    @Test
    void updateCustomer_NotFound_ThrowsException() {
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.updateCustomer(999L, validRequest))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateCustomer_NotOwner_ThrowsAccessDenied() {
        User otherUser = new User();
        otherUser.setId(99L);
        otherUser.setUsername("other");
        otherUser.setRole(Role.USER);
        authenticateAs(otherUser);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        assertThatThrownBy(() -> customerService.updateCustomer(1L, validRequest))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("your own profile");
    }

    @Test
    void updateCustomer_Orphan_AdminCanUpdate() {
        authenticateAs(adminUser);
        when(customerRepository.findById(2L)).thenReturn(Optional.of(orphanCustomer));
        when(customerRepository.existsByEmail(anyString())).thenReturn(false);
        when(customerRepository.existsByPhone(anyString())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(orphanCustomer);

        CustomerRequest updateRequest = new CustomerRequest();
        updateRequest.setName("Updated Orphan");
        updateRequest.setEmail("orphan-updated@test.com");
        updateRequest.setPhone("5551234567");

        CustomerResponse response = customerService.updateCustomer(2L, updateRequest);

        assertThat(response).isNotNull();
    }

    @Test
    void updateCustomer_DuplicateEmail_ThrowsException() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.existsByEmail(anyString())).thenReturn(true);

        CustomerRequest updateRequest = new CustomerRequest();
        updateRequest.setName("John Doe");
        updateRequest.setEmail("taken@test.com");
        updateRequest.setPhone("1234567890");

        assertThatThrownBy(() -> customerService.updateCustomer(1L, updateRequest))
                .isInstanceOf(DuplicateResourceException.class);
    }

    // Tests for patchCustomer()

    @Test
    void patchCustomer_UpdatesOnlyProvidedFields() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.existsByPhone(anyString())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        Map<String, Object> updates = new HashMap<>();
        updates.put("phone", "9998887777");

        CustomerResponse response = customerService.patchCustomer(1L, updates);

        assertThat(response).isNotNull();
        verify(customerRepository).save(any(Customer.class));
    }




    @Test
    void patchCustomer_InvalidEmail_ThrowsException() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        Map<String, Object> updates = new HashMap<>();
        updates.put("email", "not-an-email");

        assertThatThrownBy(() -> customerService.patchCustomer(1L, updates))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Email must be valid");
    }





    @Test
    void patchCustomer_EmptyName_ThrowsException() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", "");

        assertThatThrownBy(() -> customerService.patchCustomer(1L, updates))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Name cannot be null or empty");
    }






    @Test
    void patchCustomer_NotOwner_ThrowsAccessDenied() {
        User otherUser = new User();
        otherUser.setId(99L);
        otherUser.setUsername("other");
        otherUser.setRole(Role.USER);
        authenticateAs(otherUser);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", "Hacked Name");

        assertThatThrownBy(() -> customerService.patchCustomer(1L, updates))
                .isInstanceOf(AccessDeniedException.class);
    }






    // Tests for deleteCustomer()

    @Test
    void deleteCustomer_AdminSuccess() {
        authenticateAs(adminUser);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        boolean result = customerService.deleteCustomer(1L);

        assertThat(result).isTrue();
        assertThat(testCustomer.isDeleted()).isTrue();
        verify(customerRepository).save(testCustomer);
    }





    @Test
    void deleteCustomer_UserForbidden_ThrowsException() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        assertThatThrownBy(() -> customerService.deleteCustomer(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only ADMIN can delete customers");

        verify(customerRepository, never()).deleteById(anyLong());
    }




    @Test
    void deleteCustomer_NotFound_ThrowsException() {
        authenticateAs(adminUser);
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.deleteCustomer(999L))
                .isInstanceOf(NotFoundException.class);
    }





    // Test for getAllCustomersWithAddresses()

    @Test
    void getAllCustomersWithAddresses_ReturnsMappedList() {
        when(customerRepository.findAllWithAddress()).thenReturn(List.of(testCustomer));

        List<CustomerResponse> result = customerService.getAllCustomersWithAddresses();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("john@test.com");
    }
}