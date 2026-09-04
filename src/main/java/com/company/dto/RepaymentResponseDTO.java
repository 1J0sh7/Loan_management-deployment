package com.company.dto;

import java.time.LocalDate;

public class RepaymentResponseDTO {
    private Long id;
    private Double amount;
    private LocalDate dueDate;
    private LocalDate paidDate;
    private Boolean paid;
    private Double remainingBalance;

    // ✅ Constructor with 6 arguments (including remainingBalance)
    public RepaymentResponseDTO(Long id, Double amount, LocalDate dueDate, LocalDate paidDate, Boolean paid, Double remainingBalance) {
        this.id = id;
        this.amount = amount;
        this.dueDate = dueDate;
        this.paidDate = paidDate;
        this.paid = paid;
        this.remainingBalance = remainingBalance;
    }

    // Getters and Setters...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDate getPaidDate() { return paidDate; }
    public void setPaidDate(LocalDate paidDate) { this.paidDate = paidDate; }

    public Boolean getPaid() { return paid; }
    public void setPaid(Boolean paid) { this.paid = paid; }

    public Double getRemainingBalance() { return remainingBalance; }
    public void setRemainingBalance(Double remainingBalance) { this.remainingBalance = remainingBalance; }
}