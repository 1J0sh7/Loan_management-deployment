package com.company.validation;

import com.company.dto.CustomerRequest;
import com.company.dto.LoginRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationTest {
    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void customerRequestReportsRequiredAndEmailErrors() {
        CustomerRequest request = new CustomerRequest();
        request.setName("");
        request.setEmail("not-an-email");
        request.setPhone(null);
        assertThat(validator.validate(request)).extracting(v -> v.getPropertyPath().toString())
                .contains("name", "email", "phone");
    }

    @Test
    void validCustomerRequestHasNoViolations() {
        CustomerRequest request = new CustomerRequest();
        request.setName("Alice");
        request.setEmail("alice@example.com");
        request.setPhone("5551234567");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void loginRequestRequiresBothFields() {
        LoginRequest request = new LoginRequest();
        assertThat(validator.validate(request)).hasSize(2);
    }
}
