package com.company.service;

import com.company.exception.ResourceNotFoundException;
import com.company.exception.ValidationException;
import com.company.model.LoanProduct;
import com.company.repository.LoanProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoanProductService {

    private final LoanProductRepository loanProductRepository;

    public LoanProductService(LoanProductRepository loanProductRepository) {
        this.loanProductRepository = loanProductRepository;
    }

    public LoanProduct createLoanProduct(LoanProduct product) {
        // No 12-month limit — any term allowed
        if (product.getTermMonths() < 1) {
            throw new ValidationException("Loan term must be at least 1 month");
        }
        return loanProductRepository.save(product);
    }

    public Page<LoanProduct> getAllProducts(Pageable pageable) {
        return loanProductRepository.findByActiveTrue(pageable);
    }

    public LoanProduct getLoanProductById(Long id) {
        return loanProductRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan product not found with id: " + id));
    }

    public LoanProduct updateLoanProduct(Long id, LoanProduct updatedProduct) {
        LoanProduct existing = getLoanProductById(id);

        //  No 12-month limit — any term allowed
        if (updatedProduct.getTermMonths() < 1) {
            throw new ValidationException("Loan term must be at least 1 month");
        }

        existing.setName(updatedProduct.getName());
        existing.setDescription(updatedProduct.getDescription());
        existing.setInterestRate(updatedProduct.getInterestRate());
        existing.setTermMonths(updatedProduct.getTermMonths());
        existing.setMaxAmount(updatedProduct.getMaxAmount());
        existing.setActive(updatedProduct.getActive());
        return loanProductRepository.save(existing);
    }

    public void deleteLoanProduct(Long id) {
        LoanProduct product = getLoanProductById(id);
        product.setActive(false);
        loanProductRepository.save(product);
    }

    public List<LoanProduct> getActiveLoanProducts() {
        return loanProductRepository.findByActiveTrue();
    }
}