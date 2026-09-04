package com.company.repository;

import com.company.model.LoanProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {


    List<LoanProduct> findByActiveTrue();

    Page<LoanProduct> findByActiveTrue(Pageable pageable);

    Optional<LoanProduct> findByIdAndActiveTrue(Long id);
}