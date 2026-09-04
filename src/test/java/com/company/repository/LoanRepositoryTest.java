package com.company.repository;

import com.company.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LoanRepositoryTest {
    @Autowired LoanProductRepository productRepository;
    @Autowired LoanApplicationRepository applicationRepository;
    @Autowired RepaymentRepository repaymentRepository;
    @Autowired LoanApplicationHistoryRepository historyRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired UserRepository userRepository;

    @Test
    void loanProductRepositoryQueriesOnlyActiveProducts() {
        LoanProduct active = product("active", true);
        LoanProduct inactive = product("inactive", false);
        productRepository.save(active);
        productRepository.save(inactive);

        assertThat(productRepository.findByActiveTrue()).extracting(LoanProduct::getName)
                .contains("active").doesNotContain("inactive");
        assertThat(productRepository.findByIdAndActiveTrue(inactive.getId())).isEmpty();
        Page<LoanProduct> page = productRepository.findByActiveTrue(PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(LoanProduct::getName).contains("active");
    }

    @Test
    void loanApplicationAndHistoryRepositoriesSupportStatusAndOrdering() {
        User user = userRepository.save(new User("loanuser", "password", Role.USER));
        Customer customer = customerRepository.save(new Customer("Customer", "loan@example.com", "5551112222"));
        customer.setUser(user);
        customerRepository.save(customer);
        LoanProduct product = productRepository.save(product("product", true));
        LoanApplication pending = applicationRepository.save(new LoanApplication(customer, product,
                BigDecimal.valueOf(100), LoanStatus.PENDING));
        LoanApplication completed = applicationRepository.save(new LoanApplication(customer, product,
                BigDecimal.valueOf(100), LoanStatus.COMPLETED));

        assertThat(applicationRepository.existsByCustomerAndStatusIn(customer,
                List.of(LoanStatus.PENDING))).isTrue();
        Page<LoanApplication> page = applicationRepository.findByCustomer(customer,
                PageRequest.of(0, 10, Sort.by("id")));
        assertThat(page.getContent()).containsExactly(pending, completed);

        LoanApplicationHistory first = historyRepository.save(new LoanApplicationHistory(
                pending, null, LoanStatus.PENDING, "loanuser", "submitted"));
        LoanApplicationHistory second = historyRepository.save(new LoanApplicationHistory(
                pending, LoanStatus.PENDING, LoanStatus.APPROVED, "admin", "approved"));
        List<LoanApplicationHistory> history =
                historyRepository.findByLoanApplicationOrderByChangedAtDesc(pending);
        assertThat(history).containsExactly(second, first);
    }

    @Test
    void repaymentRepositoryFindsPendingRepaymentsByDueDate() {
        User user = userRepository.save(new User("repayuser", "password", Role.USER));
        Customer customer = new Customer("Repay", "repay@example.com", "5553334444");
        customer.setUser(user);
        customerRepository.save(customer);
        LoanProduct product = productRepository.save(product("repay-product", true));
        LoanApplication application = applicationRepository.save(new LoanApplication(customer, product,
                BigDecimal.valueOf(500), LoanStatus.ACTIVE));
        Repayment later = new Repayment(application, BigDecimal.TEN, LocalDate.now().plusDays(3));
        Repayment earlier = new Repayment(application, BigDecimal.TEN, LocalDate.now().plusDays(1));
        earlier.setStatus("PENDING");
        later.setStatus("PENDING");
        repaymentRepository.save(later);
        repaymentRepository.save(earlier);

        assertThat(repaymentRepository.findByLoanApplicationAndStatusOrderByDueDateAsc(application, "PENDING"))
                .containsExactly(earlier, later);
    }

    private LoanProduct product(String name, boolean active) {
        LoanProduct product = new LoanProduct(name, "description", 10.0, 12, BigDecimal.valueOf(5000));
        product.setActive(active);
        return product;
    }
}
