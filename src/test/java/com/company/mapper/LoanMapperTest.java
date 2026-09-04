package com.company.mapper;

import com.company.dto.*;
import com.company.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class LoanMapperTest {
    @Test
    void nullInputsMapToNull() {
        assertThat(LoanMapper.toCustomerResponse(null)).isNull();
        assertThat(LoanMapper.toLoanProductResponseDTO(null)).isNull();
        assertThat(LoanMapper.toLoanApplicationResponseDTO(null)).isNull();
        assertThat(LoanMapper.toRepaymentResponseDTO(null, null)).isNull();
        assertThat(LoanMapper.toHistoryResponseDTO(null)).isNull();
    }

    @Test
    void mapsLoanApplicationAndNestedValues() {
        Customer customer = new Customer("Jane", "jane@example.com", "555");
        customer.setId(1L);
        LoanProduct product = new LoanProduct("Gold", "desc", 12.5, 18, BigDecimal.valueOf(8000));
        product.setId(2L);
        LoanApplication application = new LoanApplication(customer, product, BigDecimal.valueOf(1250.50),
                LoanStatus.ACTIVE);
        application.setId(3L);
        application.setRemainingBalance(BigDecimal.valueOf(900.25));

        LoanApplicationResponseDTO dto = LoanMapper.toLoanApplicationResponseDTO(application);

        assertThat(dto.getId()).isEqualTo(3L);
        assertThat(dto.getCustomer().getEmail()).isEqualTo("jane@example.com");
        assertThat(dto.getProduct().getInterestRate()).isEqualTo(12.5);
        assertThat(dto.getAmount()).isEqualTo(1250.50);
        assertThat(dto.getRemainingBalance()).isEqualTo(900.25);
    }

    @Test
    void mapsRepaymentPaidFlagAndHistoryStatuses() {
        LoanApplication application = new LoanApplication();
        application.setId(3L);
        Repayment repayment = new Repayment(application, BigDecimal.TEN, LocalDate.now());
        repayment.setId(4L);
        repayment.setPaidDate(LocalDate.now());
        RepaymentResponseDTO repaymentDto = LoanMapper.toRepaymentResponseDTO(repayment, BigDecimal.ONE);
        assertThat(repaymentDto.getPaid()).isTrue();
        assertThat(repaymentDto.getRemainingBalance()).isEqualTo(1.0);

        LoanApplicationHistory history = new LoanApplicationHistory(application, null,
                LoanStatus.PENDING, "alice", "submitted");
        LoanApplicationHistoryResponseDTO historyDto = LoanMapper.toHistoryResponseDTO(history);
        assertThat(historyDto.getPreviousStatus()).isNull();
        assertThat(historyDto.getNewStatus()).isEqualTo("PENDING");
        assertThat(historyDto.getLoanApplicationId()).isEqualTo(3L);
    }
}
