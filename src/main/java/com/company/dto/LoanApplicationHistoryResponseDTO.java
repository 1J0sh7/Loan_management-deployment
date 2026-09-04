package com.company.dto;

import java.time.LocalDateTime;

public class LoanApplicationHistoryResponseDTO {
    private Long id;
    private Long loanApplicationId;
    private String previousStatus;
    private String newStatus;
    private String changedBy;
    private String reason;
    private LocalDateTime changedAt;

    public LoanApplicationHistoryResponseDTO() {}

    public LoanApplicationHistoryResponseDTO(Long id, Long loanApplicationId,
                                             String previousStatus, String newStatus,
                                             String changedBy, String reason,
                                             LocalDateTime changedAt) {
        this.id = id;
        this.loanApplicationId = loanApplicationId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.reason = reason;
        this.changedAt = changedAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLoanApplicationId() { return loanApplicationId; }
    public void setLoanApplicationId(Long loanApplicationId) { this.loanApplicationId = loanApplicationId; }

    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }

    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}