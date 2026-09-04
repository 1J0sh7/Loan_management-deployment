package com.company.dto;

public class LoanProductResponseDTO {
    private Long id;
    private String name;
    private String description;
    private Double interestRate;
    private Integer termMonths;
    private Double maxAmount;

    // Constructors
    public LoanProductResponseDTO() {}

    public LoanProductResponseDTO(Long id, String name, String description, Double interestRate, Integer termMonths, Double maxAmount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.interestRate = interestRate;
        this.termMonths = termMonths;
        this.maxAmount = maxAmount;
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

    public Double getMaxAmount() { return maxAmount; }
    public void setMaxAmount(Double maxAmount) { this.maxAmount = maxAmount; }
}