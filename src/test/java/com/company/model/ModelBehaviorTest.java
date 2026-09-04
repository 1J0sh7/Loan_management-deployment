package com.company.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ModelBehaviorTest {
    @Test
    void userExposesRoleAuthorityAndEnabledFlags() {
        User user = new User("admin", "password", Role.ADMIN);
        assertThat(user.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.isAccountNonExpired()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void repaymentTracksPaidStateAndDefaultsPending() {
        Repayment repayment = new Repayment();
        assertThat(repayment.getStatus()).isEqualTo("PENDING");
        assertThat(repayment.isPaid()).isFalse();
        repayment.setPaidDate(LocalDate.now());
        assertThat(repayment.isPaid()).isTrue();
    }

    @Test
    void constructorsInitializeLoanAndProductState() {
        LoanProduct product = new LoanProduct("Basic", "desc", 5.0, 6, BigDecimal.TEN);
        assertThat(product.getActive()).isTrue();
        LoanApplication application = new LoanApplication(new Customer(), product, BigDecimal.ONE,
                LoanStatus.PENDING);
        assertThat(application.getStatus()).isEqualTo(LoanStatus.PENDING);
        assertThat(application.getCreatedAt()).isNotNull();
    }
}
