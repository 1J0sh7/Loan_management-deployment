package com.company.service;

import com.company.dto.LoanApplicationHistoryResponseDTO;
import com.company.model.Customer;
import com.company.model.LoanApplication;
import com.company.model.LoanApplicationHistory;
import com.company.model.LoanProduct;
import com.company.model.LoanStatus;
import com.company.model.User;
import com.company.model.Role;
import com.company.repository.CustomerRepository;
import com.company.repository.LoanApplicationHistoryRepository;
import com.company.repository.LoanApplicationRepository;
import com.company.repository.LoanProductRepository;
import com.company.repository.RepaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanApplicationAuditTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private LoanProductRepository loanProductRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private RepaymentRepository repaymentRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private LoanApplicationHistoryRepository historyRepository;

    @InjectMocks
    private LoanApplicationService loanApplicationService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void approveLoan_RecordsStatusChangeAndAdmin() {
        when(loanApplicationRepository.save(any(LoanApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.save(any(LoanApplicationHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoanApplication application = application(LoanStatus.PENDING);
        when(loanApplicationRepository.findById(1L)).thenReturn(Optional.of(application));

        loanApplicationService.approveLoan(1L, "admin01");

        ArgumentCaptor<LoanApplicationHistory> captor =
                ArgumentCaptor.forClass(LoanApplicationHistory.class);
        verify(historyRepository).save(captor.capture());

        LoanApplicationHistory history = captor.getValue();
        assertThat(application.getStatus()).isEqualTo(LoanStatus.APPROVED);
        assertThat(history.getPreviousStatus()).isEqualTo(LoanStatus.PENDING);
        assertThat(history.getNewStatus()).isEqualTo(LoanStatus.APPROVED);
        assertThat(history.getChangedBy()).isEqualTo("admin01");
        assertThat(history.getChangedAt()).isNotNull();
    }

    @Test
    void disburseLoan_RecordsAuthenticatedAdminAndInitialBalance() {
        when(loanApplicationRepository.save(any(LoanApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.save(any(LoanApplicationHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authenticateAs("admin01", Role.ADMIN);
        LoanApplication application = application(LoanStatus.APPROVED);
        when(loanApplicationRepository.findById(1L)).thenReturn(Optional.of(application));

        loanApplicationService.disburseLoan(1L);

        ArgumentCaptor<LoanApplicationHistory> captor =
                ArgumentCaptor.forClass(LoanApplicationHistory.class);
        verify(historyRepository).save(captor.capture());

        assertThat(application.getStatus()).isEqualTo(LoanStatus.DISBURSED);
        assertThat(application.getRemainingBalance()).isEqualByComparingTo("1100.0000000000");
        assertThat(captor.getValue().getChangedBy()).isEqualTo("admin01");
        assertThat(captor.getValue().getPreviousStatus()).isEqualTo(LoanStatus.APPROVED);
        assertThat(captor.getValue().getNewStatus()).isEqualTo(LoanStatus.DISBURSED);
    }

    @Test
    void getLoanHistory_ReturnsSafeAuditDtos() {
        LoanApplication application = application(LoanStatus.ACTIVE);
        LoanApplicationHistory history = new LoanApplicationHistory(
                application,
                LoanStatus.DISBURSED,
                LoanStatus.ACTIVE,
                "customer01",
                "First repayment made"
        );
        when(loanApplicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(historyRepository.findByLoanApplicationOrderByChangedAtDesc(application))
                .thenReturn(List.of(history));

        List<LoanApplicationHistoryResponseDTO> result =
                loanApplicationService.getLoanHistory(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLoanApplicationId()).isEqualTo(1L);
        assertThat(result.get(0).getPreviousStatus()).isEqualTo("DISBURSED");
        assertThat(result.get(0).getNewStatus()).isEqualTo("ACTIVE");
        assertThat(result.get(0).getChangedBy()).isEqualTo("customer01");
    }

    private LoanApplication application(LoanStatus status) {
        User user = new User("customer01", "password", Role.USER);
        Customer customer = new Customer("Customer", "customer@example.com", "1234567890");
        customer.setId(10L);
        customer.setUser(user);

        LoanProduct product = new LoanProduct(
                "Standard",
                "Standard loan",
                10.0,
                6,
                BigDecimal.valueOf(5000)
        );
        product.setId(20L);

        LoanApplication application = new LoanApplication(
                customer,
                product,
                BigDecimal.valueOf(1000),
                status
        );
        application.setId(1L);
        application.setCreatedAt(LocalDateTime.now());
        return application;
    }

    private void authenticateAs(String username, Role role) {
        User user = new User(username, "password", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }
}
