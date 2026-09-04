package com.company.dto;

import com.company.model.LoanStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class LoanApplicationResponseDTO {
    private Long id;
    private CustomerResponse customer;          // Uses YOUR existing CustomerResponse
    private LoanProductResponseDTO product;
    private Double amount;
    private LoanStatus status;
    private LocalDateTime createdAt;
    private LocalDate approvedDate;
    private LocalDate disbursedDate;
    private Double remainingBalance;

    // Constructors
    public LoanApplicationResponseDTO() {}

    public LoanApplicationResponseDTO(Long id, CustomerResponse customer, LoanProductResponseDTO product,
                                      Double amount, LoanStatus status, LocalDateTime createdAt,
                                      LocalDate approvedDate, LocalDate disbursedDate, Double remainingBalance) {
        this.id = id;
        this.customer = customer;
        this.product = product;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.approvedDate = approvedDate;
        this.disbursedDate = disbursedDate;
        this.remainingBalance = remainingBalance;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CustomerResponse getCustomer() { return customer; }
    public void setCustomer(CustomerResponse customer) { this.customer = customer; }

    public LoanProductResponseDTO getProduct() { return product; }
    public void setProduct(LoanProductResponseDTO product) { this.product = product; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public LoanStatus getStatus() { return status; }
    public void setStatus(LoanStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDate getApprovedDate() { return approvedDate; }
    public void setApprovedDate(LocalDate approvedDate) { this.approvedDate = approvedDate; }

    public LocalDate getDisbursedDate() { return disbursedDate; }
    public void setDisbursedDate(LocalDate disbursedDate) { this.disbursedDate = disbursedDate; }

    public Double getRemainingBalance() { return remainingBalance; }
    public void setRemainingBalance(Double remainingBalance) { this.remainingBalance = remainingBalance; }
}