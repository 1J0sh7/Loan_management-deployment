package com.company.controller;

import com.company.dto.*;
import com.company.model.LoanStatus;
import com.company.service.LoanApplicationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanApplicationControllerTest {
    @Mock LoanApplicationService service;
    @Mock Authentication authentication;
    @InjectMocks LoanApplicationController controller;

    @Test
    void applyAndRepayConvertAmountsAndReturnCreated() {
        LoanApplicationResponseDTO application = new LoanApplicationResponseDTO();
        application.setStatus(LoanStatus.PENDING);
        RepaymentResponseDTO repayment = new RepaymentResponseDTO(2L, 100.0, null, null, true, 900.0);
        when(service.applyForLoan(1L, 2L, BigDecimal.valueOf(100.0))).thenReturn(application);
        when(service.makeRepayment(3L, BigDecimal.valueOf(100.0))).thenReturn(repayment);

        assertThat(controller.applyForLoan(1L, 2L, 100.0).getStatusCode().value()).isEqualTo(201);
        assertThat(controller.makeRepayment(3L, 100.0).getStatusCode().value()).isEqualTo(201);
        verify(service).applyForLoan(1L, 2L, BigDecimal.valueOf(100.0));
        verify(service).makeRepayment(3L, BigDecimal.valueOf(100.0));
    }

    @Test
    void routesUserAndAdminReadsAndAdminActions() {
        when(authentication.getName()).thenReturn("alice");
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(authentication).getAuthorities();
        Page<LoanApplicationResponseDTO> page = new PageImpl<>(List.of(new LoanApplicationResponseDTO()));
        when(service.getApplicationByIdForUser(eq(1L), eq("alice"))).thenReturn(new LoanApplicationResponseDTO());
        when(service.getApplicationsByCustomer(eq(2L), any(Pageable.class), eq("alice"))).thenReturn(page);
        assertThat(controller.getApplication(1L, authentication).getStatusCode().value()).isEqualTo(200);
        assertThat(controller.getApplicationsByCustomer(2L, 0, 10, "createdAt", "desc", authentication)
                .getBody()).isSameAs(page);

        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();
        when(service.getApplicationById(1L)).thenReturn(new LoanApplicationResponseDTO());
        when(service.getApplicationsByCustomerForAdmin(eq(2L), any(Pageable.class))).thenReturn(page);
        assertThat(controller.getApplication(1L, authentication).getStatusCode().value()).isEqualTo(200);
        controller.getApplicationsByCustomer(2L, 0, 10, "createdAt", "desc", authentication);
        verify(service).getApplicationsByCustomerForAdmin(eq(2L), any(Pageable.class));
    }

    @Test
    void adminCommandsAndHistoryDelegateWithCurrentUser() {
        when(authentication.getName()).thenReturn("admin");
        LoanApplicationResponseDTO dto = new LoanApplicationResponseDTO();
        when(service.approveLoan(1L, "admin")).thenReturn(dto);
        when(service.rejectLoan(1L, "reason")).thenReturn(dto);
        when(service.disburseLoan(1L)).thenReturn(dto);
        when(service.getLoanHistory(1L)).thenReturn(List.of());
        assertThat(controller.approveLoan(1L, authentication).getBody()).isSameAs(dto);
        assertThat(controller.rejectLoan(1L, "reason").getBody()).isSameAs(dto);
        assertThat(controller.disburseLoan(1L).getBody()).isSameAs(dto);
        assertThat(controller.getLoanHistory(1L).getBody()).isEmpty();
    }
}
