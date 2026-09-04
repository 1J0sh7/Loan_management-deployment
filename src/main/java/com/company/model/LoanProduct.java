package com.company.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "loan_products")
public class LoanProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false, columnDefinition = "NUMERIC(5,2)")   // <-- ADD THIS
    private Double interestRate;

    @Column(nullable = false)
    private Integer termMonths;

    @Column(name = "max_amount", nullable = false, columnDefinition = "NUMERIC(19,2)")   // <-- ADD THIS
    private BigDecimal maxAmount;

    @Column(nullable = false)
    private Boolean active = true;

    // Constructors
    public LoanProduct() {}

    public LoanProduct(String name, String description, Double interestRate, Integer termMonths, BigDecimal maxAmount) {
        this.name = name;
        this.description = description;
        this.interestRate = interestRate;
        this.termMonths = termMonths;
        this.maxAmount = maxAmount;
        this.active = true;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getInterestRate() { return interestRate; }
    public void setInterestRate(Double interestRate) { this.interestRate = interestRate; }

    public Integer getTermMonths() { return termMonths; }
    public void setTermMonths(Integer termMonths) { this.termMonths = termMonths; }

    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}