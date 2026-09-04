package com.company.repository;

import com.company.model.Customer;
import com.company.model.Role;
import com.company.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setRole(Role.USER);
        testUser = userRepository.save(testUser);
    }

    @Test
    void saveCustomer_ShouldGenerateId() {
        Customer customer = new Customer();
        customer.setName("John Doe");
        customer.setEmail("john@test.com");
        customer.setPhone("1234567890");
        customer.setUser(testUser);

        Customer saved = customerRepository.save(customer);

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void findByEmail_ShouldReturnCustomer() {
        Customer customer = new Customer();
        customer.setName("John Doe");
        customer.setEmail("john@test.com");
        customer.setPhone("1234567890");
        customer.setUser(testUser);
        customerRepository.save(customer);

        Optional<Customer> found = customerRepository.findByEmail("john@test.com");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("John Doe");
    }

    @Test
    void findByEmail_ShouldReturnEmpty_WhenNotExists() {
        Optional<Customer> found = customerRepository.findByEmail("nonexistent@test.com");

        assertThat(found).isEmpty();
    }

    @Test
    void existsByEmail_ShouldReturnTrue() {
        Customer customer = new Customer();
        customer.setName("John Doe");
        customer.setEmail("john@test.com");
        customer.setPhone("1234567890");
        customer.setUser(testUser);
        customerRepository.save(customer);

        boolean exists = customerRepository.existsByEmail("john@test.com");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_ShouldReturnFalse_WhenNotExists() {
        boolean exists = customerRepository.existsByEmail("nonexistent@test.com");

        assertThat(exists).isFalse();
    }

    @Test
    void existsByUserId_ShouldReturnTrue() {
        Customer customer = new Customer();
        customer.setName("John Doe");
        customer.setEmail("john@test.com");
        customer.setPhone("1234567890");
        customer.setUser(testUser);
        customerRepository.save(customer);

        boolean exists = customerRepository.existsByUserId(testUser.getId());

        assertThat(exists).isTrue();
    }

    @Test
    void existsByUserId_ShouldReturnFalse_WhenUserHasNoProfile() {
        User newUser = new User();
        newUser.setUsername("noprofile");
        newUser.setPassword("password");
        newUser.setRole(Role.USER);
        newUser = userRepository.save(newUser);

        boolean exists = customerRepository.existsByUserId(newUser.getId());

        assertThat(exists).isFalse();
    }
}