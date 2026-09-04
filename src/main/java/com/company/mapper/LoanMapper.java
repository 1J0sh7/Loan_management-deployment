package com.company.mapper;

import com.company.dto.*;
import com.company.model.Customer;
import com.company.model.LoanApplication;
import com.company.model.LoanProduct;
import com.company.model.Repayment;
import com.company.model.LoanApplicationHistory;

import java.math.BigDecimal;

public class LoanMapper {

    public static CustomerResponse toCustomerResponse(Customer customer) {
        if (customer == null) return null;
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone()
        );
    }

    public static LoanProductResponseDTO toLoanProductResponseDTO(LoanProduct product) {
        if (product == null) return null;
        return new LoanProductResponseDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getInterestRate().doubleValue(),
                product.getTermMonths(),
                product.getMaxAmount().doubleValue()
        );
    }

    public static LoanApplicationResponseDTO toLoanApplicationResponseDTO(LoanApplication application) {
        if (application == null) return null;
        return new LoanApplicationResponseDTO(
                application.getId(),
                toCustomerResponse(application.getCustomer()),
                toLoanProductResponseDTO(application.getProduct()),
                application.getAmount().doubleValue(),
                application.getStatus(),
                application.getCreatedAt(),
                application.getApprovedDate(),
                application.getDisbursedDate(),
                application.getRemainingBalance() != null ? application.getRemainingBalance().doubleValue() : null
        );
    }

    public static RepaymentResponseDTO toRepaymentResponseDTO(Repayment repayment, BigDecimal remainingBalance) {
        if (repayment == null) return null;
        return new RepaymentResponseDTO(
                repayment.getId(),
                repayment.getAmount().doubleValue(),
                repayment.getDueDate(),
                repayment.getPaidDate(),
                repayment.isPaid(),
                remainingBalance != null ? remainingBalance.doubleValue() : 0.0
        );
    }

    public static LoanApplicationHistoryResponseDTO toHistoryResponseDTO(LoanApplicationHistory history) {
        if (history == null) return null;
        return new LoanApplicationHistoryResponseDTO(
                history.getId(),
                history.getLoanApplication().getId(),
                history.getPreviousStatus() != null ? history.getPreviousStatus().name() : null,
                history.getNewStatus().name(),
                history.getChangedBy(),
                history.getReason(),
                history.getChangedAt()
        );
    }
}