package com.company.repository;

import com.company.model.LoanApplication;
import com.company.model.Repayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepaymentRepository extends JpaRepository<Repayment, Long> {
    void deleteByLoanApplication(LoanApplication loanApplication);

    //  Hybrid repayment method — finds next pending repayment by due date
    List<Repayment> findByLoanApplicationAndStatusOrderByDueDateAsc(LoanApplication application, String status);
}