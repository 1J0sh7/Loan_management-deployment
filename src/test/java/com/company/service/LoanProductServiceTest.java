package com.company.service;

import com.company.exception.ResourceNotFoundException;
import com.company.exception.ValidationException;
import com.company.model.LoanProduct;
import com.company.repository.LoanProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanProductServiceTest {

    @Mock
    private LoanProductRepository loanProductRepository;

    @InjectMocks
    private LoanProductService loanProductService;

    @Test
    void getLoanProductById_HidesInactiveProduct() {
        when(loanProductRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanProductService.getLoanProductById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Loan product not found");
    }

    @Test
    void deleteLoanProduct_SoftDeletesActiveProduct() {
        LoanProduct product = product(true);
        when(loanProductRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(product));

        loanProductService.deleteLoanProduct(1L);

        assertThat(product.getActive()).isFalse();
        verify(loanProductRepository).save(product);
    }

    @Test
    void deleteLoanProduct_ReturnsNotFoundForAlreadyInactiveProduct() {
        when(loanProductRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanProductService.deleteLoanProduct(1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(loanProductRepository, never()).save(any(LoanProduct.class));
    }

    @Test
    void createLoanProduct_acceptsAnyPositiveTermAndSaves() {
        LoanProduct product = product(true);
        product.setTermMonths(24);
        when(loanProductRepository.save(product)).thenReturn(product);

        assertThat(loanProductService.createLoanProduct(product)).isSameAs(product);
        verify(loanProductRepository).save(product);
    }

    @Test
    void createAndUpdateLoanProduct_rejectNonPositiveTerms() {
        LoanProduct product = product(true);
        product.setTermMonths(0);
        assertThatThrownBy(() -> loanProductService.createLoanProduct(product))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("at least 1");

        when(loanProductRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(product));
        assertThatThrownBy(() -> loanProductService.updateLoanProduct(1L, product))
                .isInstanceOf(ValidationException.class);
        verify(loanProductRepository, never()).save(any());
    }

    @Test
    void getAllProductsAndActiveProducts_delegateToRepository() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<LoanProduct> page = new PageImpl<>(List.of(product(true)));
        when(loanProductRepository.findByActiveTrue(pageable)).thenReturn(page);
        when(loanProductRepository.findByActiveTrue()).thenReturn(page.getContent());

        assertThat(loanProductService.getAllProducts(pageable)).isSameAs(page);
        assertThat(loanProductService.getActiveLoanProducts()).containsExactlyElementsOf(page.getContent());
    }

    @Test
    void updateLoanProduct_copiesAllEditableFields() {
        LoanProduct existing = product(true);
        LoanProduct updated = new LoanProduct("Premium", "Updated", 7.5, 36,
                BigDecimal.valueOf(10000));
        updated.setActive(false);
        when(loanProductRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(existing));
        when(loanProductRepository.save(existing)).thenReturn(existing);

        assertThat(loanProductService.updateLoanProduct(1L, updated)).isSameAs(existing);
        assertThat(existing.getName()).isEqualTo("Premium");
        assertThat(existing.getDescription()).isEqualTo("Updated");
        assertThat(existing.getInterestRate()).isEqualTo(7.5);
        assertThat(existing.getTermMonths()).isEqualTo(36);
        assertThat(existing.getMaxAmount()).isEqualByComparingTo("10000");
        assertThat(existing.getActive()).isFalse();
    }

    private LoanProduct product(boolean active) {
        LoanProduct product = new LoanProduct(
                "Standard",
                "Standard loan",
                10.0,
                6,
                BigDecimal.valueOf(5000)
        );
        product.setId(1L);
        product.setActive(active);
        return product;
    }
}
