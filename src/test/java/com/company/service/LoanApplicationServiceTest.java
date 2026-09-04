package com.company.service;

import com.company.dto.LoanApplicationResponseDTO;
import com.company.dto.RepaymentResponseDTO;
import com.company.exception.*;
import com.company.model.*;
import com.company.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceTest {

    @Mock LoanApplicationRepository applicationRepository;
    @Mock LoanProductRepository productRepository;
    @Mock CustomerRepository customerRepository;
    @Mock RepaymentRepository repaymentRepository;
    @Mock EmailService emailService;
    @Mock LoanApplicationHistoryRepository historyRepository;
    @InjectMocks LoanApplicationService service;

    private User customerUser;
    private Customer customer;
    private LoanProduct product;

    @BeforeEach
    void setUp() {
        customerUser = new User("customer", "password", Role.USER);
        customerUser.setId(1L);
        customer = new Customer("Customer", "customer@example.com", "5550001111");
        customer.setId(10L);
        customer.setUser(customerUser);
        product = new LoanProduct("Standard", "Personal loan", 10.0, 12, BigDecimal.valueOf(5000));
        product.setId(20L);
        authenticateAs(customerUser);
        lenient().when(historyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void applyForLoan_createsPendingApplicationAndInitialAudit() {
        LoanApplication saved = application(LoanStatus.PENDING, BigDecimal.valueOf(1000));
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(applicationRepository.existsByCustomerAndStatusIn(eq(customer), anyList())).thenReturn(false);
        when(productRepository.findById(20L)).thenReturn(Optional.of(product));
        when(applicationRepository.save(any())).thenReturn(saved);

        LoanApplicationResponseDTO result = service.applyForLoan(10L, 20L, BigDecimal.valueOf(1000));

        assertThat(result.getStatus()).isEqualTo(LoanStatus.PENDING);
        assertThat(result.getAmount()).isEqualTo(1000.0);
        verify(historyRepository).save(argThat(h -> h.getPreviousStatus() == null
                && h.getNewStatus() == LoanStatus.PENDING
                && h.getChangedBy().equals("customer")));
        verify(emailService).sendLoanCreationEmail(customer.getEmail(), saved);
    }

    @Test
    void applyForLoan_rejectsCustomerWithActiveLoan() {
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(applicationRepository.existsByCustomerAndStatusIn(eq(customer), anyList())).thenReturn(true);

        assertThatThrownBy(() -> service.applyForLoan(10L, 20L, BigDecimal.TEN))
                .isInstanceOf(CustomerHasActiveLoanException.class);
        verifyNoInteractions(productRepository, emailService);
    }

    @Test
    void applyForLoan_rejectsWrongOwnerAndMissingProductAndExcessAmount() {
        User other = new User("other", "password", Role.USER);
        customer.setUser(other);
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        assertThatThrownBy(() -> service.applyForLoan(10L, 20L, BigDecimal.TEN))
                .isInstanceOf(ResourceNotFoundException.class);

        customer.setUser(customerUser);
        when(applicationRepository.existsByCustomerAndStatusIn(any(), anyList())).thenReturn(false);
        when(productRepository.findById(20L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.applyForLoan(10L, 20L, BigDecimal.TEN))
                .isInstanceOf(ResourceNotFoundException.class);

        when(productRepository.findById(20L)).thenReturn(Optional.of(product));
        assertThatThrownBy(() -> service.applyForLoan(10L, 20L, BigDecimal.valueOf(5001)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("maximum");
    }

    @Test
    void approveLoan_setsApprovalMetadataAndAudits() {
        LoanApplication app = application(LoanStatus.PENDING, BigDecimal.valueOf(1000));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        LoanApplicationResponseDTO result = service.approveLoan(1L, "admin");

        assertThat(result.getStatus()).isEqualTo(LoanStatus.APPROVED);
        assertThat(app.getApprovedBy()).isEqualTo("admin");
        assertThat(app.getApprovedDate()).isEqualTo(LocalDate.now());
        verify(historyRepository).save(argThat(h -> h.getPreviousStatus() == LoanStatus.PENDING
                && h.getNewStatus() == LoanStatus.APPROVED && h.getChangedBy().equals("admin")));
        verify(emailService).sendLoanApprovalEmail(customer.getEmail(), app);
    }

    @Test
    void approveLoan_requiresPendingStatus() {
        LoanApplication app = application(LoanStatus.APPROVED, BigDecimal.valueOf(1000));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        assertThatThrownBy(() -> service.approveLoan(1L, "admin"))
                .isInstanceOf(LoanNotPendingException.class);
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void rejectLoan_setsReasonAndAuthenticatedAdmin() {
        LoanApplication app = application(LoanStatus.PENDING, BigDecimal.valueOf(1000));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        User admin = new User("admin", "password", Role.ADMIN);
        authenticateAs(admin);

        LoanApplicationResponseDTO result = service.rejectLoan(1L, "Income not verified");

        assertThat(result.getStatus()).isEqualTo(LoanStatus.REJECTED);
        assertThat(app.getRejectionReason()).isEqualTo("Income not verified");
        verify(historyRepository).save(argThat(h -> h.getChangedBy().equals("admin")
                && h.getReason().equals("Income not verified")));
        verify(emailService).sendLoanRejectionEmail(customer.getEmail(), app, "Income not verified");
    }

    @Test
    void disburseLoan_calculatesInterestAndInitialBalance() {
        LoanApplication app = application(LoanStatus.APPROVED, BigDecimal.valueOf(1000));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        User admin = new User("admin", "password", Role.ADMIN);
        authenticateAs(admin);

        LoanApplicationResponseDTO result = service.disburseLoan(1L);

        assertThat(result.getStatus()).isEqualTo(LoanStatus.DISBURSED);
        assertThat(app.getRemainingBalance()).isEqualByComparingTo("1100.0000000000");
        assertThat(app.getDisbursedDate()).isEqualTo(LocalDate.now());
        verify(emailService).sendLoanDisbursementEmail(customer.getEmail(), app);
    }

    @Test
    void disburseLoan_requiresApprovedStatus() {
        LoanApplication app = application(LoanStatus.PENDING, BigDecimal.valueOf(1000));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        assertThatThrownBy(() -> service.disburseLoan(1L))
                .isInstanceOf(LoanNotApprovedException.class);
    }

    @Test
    void makeRepayment_partialPaymentActivatesLoanAndPersistsPaidRepayment() {
        LoanApplication app = application(LoanStatus.DISBURSED, BigDecimal.valueOf(1000));
        app.setDisbursedDate(LocalDate.now());
        app.setRemainingBalance(BigDecimal.valueOf(1000));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(repaymentRepository.save(any())).thenAnswer(i -> {
            Repayment repayment = i.getArgument(0);
            repayment.setId(2L);
            return repayment;
        });
        when(applicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RepaymentResponseDTO result = service.makeRepayment(1L, BigDecimal.valueOf(100));

        assertThat(result.getAmount()).isEqualTo(100.0);
        assertThat(result.getRemainingBalance()).isEqualTo(900.0);
        assertThat(app.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        ArgumentCaptor<Repayment> captor = ArgumentCaptor.forClass(Repayment.class);
        verify(repaymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("PAID");
        assertThat(captor.getValue().isPaid()).isTrue();
        verify(emailService).sendRepaymentConfirmationEmail(customer.getEmail(), app, captor.getValue());
    }

    @Test
    void makeRepayment_fullPaymentCompletesLoan() {
        LoanApplication app = application(LoanStatus.ACTIVE, BigDecimal.valueOf(1000));
        app.setDisbursedDate(LocalDate.now());
        app.setRemainingBalance(BigDecimal.valueOf(1000));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(repaymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(applicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RepaymentResponseDTO result = service.makeRepayment(1L, BigDecimal.valueOf(1000));

        assertThat(result.getRemainingBalance()).isZero();
        assertThat(app.getStatus()).isEqualTo(LoanStatus.COMPLETED);
        verify(historyRepository).save(argThat(h -> h.getNewStatus() == LoanStatus.COMPLETED
                && h.getReason().equals("Loan fully repaid")));
    }

    @Test
    void makeRepayment_rejectsInvalidAmountsAndStatuses() {
        LoanApplication app = application(LoanStatus.ACTIVE, BigDecimal.valueOf(1000));
        app.setDisbursedDate(LocalDate.now());
        app.setRemainingBalance(BigDecimal.valueOf(1000));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> service.makeRepayment(1L, BigDecimal.ZERO))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.makeRepayment(1L, BigDecimal.valueOf(1001)))
                .isInstanceOf(InsufficientBalanceException.class);

        app.setStatus(LoanStatus.PENDING);
        assertThatThrownBy(() -> service.makeRepayment(1L, BigDecimal.TEN))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void makeRepayment_rejectsBelowDynamicMinimumAndWrongOwner() {
        LoanApplication app = application(LoanStatus.ACTIVE, BigDecimal.valueOf(1200));
        app.setDisbursedDate(LocalDate.now());
        app.setRemainingBalance(BigDecimal.valueOf(1200));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> service.makeRepayment(1L, BigDecimal.valueOf(99)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("minimum monthly payment");

        customer.setUser(new User("other", "password", Role.USER));
        assertThatThrownBy(() -> service.makeRepayment(1L, BigDecimal.valueOf(100)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getApplicationByIdForUser_enforcesOwnershipAndMapsResult() {
        LoanApplication app = application(LoanStatus.PENDING, BigDecimal.valueOf(100));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

        assertThat(service.getApplicationByIdForUser(1L, "customer").getId()).isEqualTo(1L);
        assertThatThrownBy(() -> service.getApplicationByIdForUser(1L, "other"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listMethods_filterByCustomerAndMapPages() {
        LoanApplication app = application(LoanStatus.PENDING, BigDecimal.valueOf(100));
        Page<LoanApplication> page = new PageImpl<>(List.of(app));
        PageRequest pageable = PageRequest.of(0, 10);
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(applicationRepository.findByCustomer(customer, pageable)).thenReturn(page);
        when(applicationRepository.findAll(pageable)).thenReturn(page);

        assertThat(service.getApplicationsByCustomer(10L, pageable, "customer").getContent()).hasSize(1);
        assertThat(service.getApplicationsByCustomerForAdmin(10L, pageable).getContent()).hasSize(1);
        assertThat(service.getAllApplications(pageable).getContent()).hasSize(1);
        verify(applicationRepository).findAll(pageable);
    }

    @Test
    void overdueApplicationIsMarkedAndAuditedWhenRead() {
        LoanApplication app = application(LoanStatus.ACTIVE, BigDecimal.valueOf(100));
        app.setDisbursedDate(LocalDate.now().minusMonths(13));
        app.setRemainingBalance(BigDecimal.TEN);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        LoanApplicationResponseDTO result = service.getApplicationById(1L);

        assertThat(result.getStatus()).isEqualTo(LoanStatus.OVERDUE);
        verify(historyRepository).save(argThat(h -> h.getChangedBy().equals("SYSTEM")
                && h.getNewStatus() == LoanStatus.OVERDUE));
    }

    @Test
    void emailFailuresDoNotRollbackLoanOperation() {
        LoanApplication app = application(LoanStatus.PENDING, BigDecimal.valueOf(100));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("mail unavailable")).when(emailService)
                .sendLoanApprovalEmail(anyString(), any());

        assertThat(service.approveLoan(1L, "admin").getStatus()).isEqualTo(LoanStatus.APPROVED);
        verify(applicationRepository, atLeastOnce()).save(app);
    }

    private LoanApplication application(LoanStatus status, BigDecimal amount) {
        LoanApplication app = new LoanApplication(customer, product, amount, status);
        app.setId(1L);
        app.setCreatedAt(LocalDateTime.now());
        return app;
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
