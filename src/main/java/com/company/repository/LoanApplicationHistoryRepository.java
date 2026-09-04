package com.company.repository;

import com.company.model.LoanApplication;
import com.company.model.LoanApplicationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanApplicationHistoryRepository extends JpaRepository<LoanApplicationHistory, Long> {
    List<LoanApplicationHistory> findByLoanApplicationOrderByChangedAtDesc(LoanApplication loanApplication);
}