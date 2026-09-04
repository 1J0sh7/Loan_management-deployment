package com.company.repository;

import com.company.model.Customer;
import com.company.model.LoanApplication;
import com.company.model.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
    Page<LoanApplication> findByCustomer(Customer customer, Pageable pageable);

    // ===== THIS METHOD MUST EXIST =====
    boolean existsByCustomerAndStatusIn(Customer customer, List<LoanStatus> statuses);
}