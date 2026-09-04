package com.company.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_application_history")
public class LoanApplicationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "loan_application_id", nullable = false)
    private LoanApplication loanApplication;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)  // ✅ Allows null for initial PENDING record
    private LoanStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus newStatus;

    @Column(nullable = false)
    private String changedBy;

    private String reason;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    // Constructors
    public LoanApplicationHistory() {}

    public LoanApplicationHistory(LoanApplication loanApplication, LoanStatus previousStatus,
                                  LoanStatus newStatus, String changedBy, String reason) {
        this.loanApplication = loanApplication;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.reason = reason;
        this.changedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LoanApplication getLoanApplication() { return loanApplication; }
    public void setLoanApplication(LoanApplication loanApplication) { this.loanApplication = loanApplication; }

    public LoanStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(LoanStatus previousStatus) { this.previousStatus = previousStatus; }

    public LoanStatus getNewStatus() { return newStatus; }
    public void setNewStatus(LoanStatus newStatus) { this.newStatus = newStatus; }

    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}