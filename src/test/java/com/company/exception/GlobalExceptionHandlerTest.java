package com.company.exception;

import com.company.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsDomainExceptionsToExpectedStatusAndMessage() {
        ResponseEntity<ErrorResponse> duplicate =
                handler.handleDuplicateResource(new DuplicateResourceException("duplicate"));
        assertThat(duplicate.getStatusCode().value()).isEqualTo(409);
        assertThat(duplicate.getBody().getMessage()).isEqualTo("duplicate");

        assertThat(handler.handleNotFound(new NotFoundException("missing")).getStatusCode().value()).isEqualTo(404);
        assertThat(handler.handleValidation(new ValidationException("bad")).getStatusCode().value()).isEqualTo(400);
        assertThat(handler.handleAccessDenied(new AccessDeniedException("forbidden")).getStatusCode().value()).isEqualTo(403);
        assertThat(handler.handleCustomerHasActiveLoan(new CustomerHasActiveLoanException("active"))
                .getStatusCode().value()).isEqualTo(409);
        assertThat(handler.handleLoanNotPending(new LoanNotPendingException("pending"))
                .getStatusCode().value()).isEqualTo(400);
        assertThat(handler.handleLoanNotApproved(new LoanNotApprovedException("approved"))
                .getStatusCode().value()).isEqualTo(400);
        assertThat(handler.handleInsufficientBalance(new InsufficientBalanceException("balance"))
                .getStatusCode().value()).isEqualTo(400);
        assertThat(handler.handleResourceNotFound(new ResourceNotFoundException("missing"))
                .getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void mapsUnexpectedExceptionToSafe500Response() {
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(new IllegalStateException("secret"));
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).doesNotContain("secret");
    }
}
