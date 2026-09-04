package com.company.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loan_applications")
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private LoanProduct product;

    @Column(nullable = false, columnDefinition = "NUMERIC(19,2)")
    private BigDecimal amount;   // <-- Changed from Double

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "approved_date")
    private LocalDate approvedDate;

    @Column(name = "disbursed_date")
    private LocalDate disbursedDate;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "remaining_balance", columnDefinition = "NUMERIC(19,2)")
    private BigDecimal remainingBalance;   // <-- Changed from Double

    @OneToMany(mappedBy = "loanApplication", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Repayment> repayments = new ArrayList<>();

    // Constructors
    public LoanApplication() {}

    public LoanApplication(Customer customer, LoanProduct product, BigDecimal amount, LoanStatus status) {
        this.customer = customer;
        this.product = product;
        this.amount = amount;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public LoanProduct getProduct() { return product; }
    public void setProduct(LoanProduct product) { this.product = product; }

    public BigDecimal getAmount() { return amount; }   // <-- Changed return type
    public void setAmount(BigDecimal amount) { this.amount = amount; }   // <-- Changed param type

    public LoanStatus getStatus() { return status; }
    public void setStatus(LoanStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDate getApprovedDate() { return approvedDate; }
    public void setApprovedDate(LocalDate approvedDate) { this.approvedDate = approvedDate; }

    public LocalDate getDisbursedDate() { return disbursedDate; }
    public void setDisbursedDate(LocalDate disbursedDate) { this.disbursedDate = disbursedDate; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public BigDecimal getRemainingBalance() { return remainingBalance; }   // <-- Changed return type
    public void setRemainingBalance(BigDecimal remainingBalance) { this.remainingBalance = remainingBalance; }   // <-- Changed param type

    public List<Repayment> getRepayments() { return repayments; }
    public void setRepayments(List<Repayment> repayments) { this.repayments = repayments; }
}