package com.company.controller;

import com.company.model.LoanProduct;
import com.company.service.LoanProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanProductControllerTest {
    @Mock LoanProductService service;
    @InjectMocks LoanProductController controller;

    @Test
    void getAllProductsBuildsPageAndMapsDto() {
        LoanProduct product = product();
        when(service.getAllProducts(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));
        ResponseEntity<Page<com.company.dto.LoanProductResponseDTO>> response =
                controller.getAllProducts(1, 5, "name", "desc");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getContent().get(0).getName()).isEqualTo("Standard");
        verify(service).getAllProducts(argThat(p -> p.getPageNumber() == 1
                && p.getPageSize() == 5 && p.getSort().getOrderFor("name").isDescending()));
    }

    @Test
    void getByIdCreateUpdateAndDeleteDelegate() {
        LoanProduct product = product();
        when(service.getLoanProductById(1L)).thenReturn(product);
        when(service.createLoanProduct(product)).thenReturn(product);
        when(service.updateLoanProduct(1L, product)).thenReturn(product);

        assertThat(controller.getProductById(1L).getBody().getId()).isEqualTo(1L);
        assertThat(controller.createLoanProduct(product).getBody()).isSameAs(product);
        assertThat(controller.updateLoanProduct(1L, product).getBody()).isSameAs(product);
        assertThat(controller.deleteLoanProduct(1L).getStatusCode().value()).isEqualTo(204);
        verify(service).deleteLoanProduct(1L);
    }

    private LoanProduct product() {
        LoanProduct product = new LoanProduct("Standard", "desc", 10.0, 12, BigDecimal.valueOf(5000));
        product.setId(1L);
        return product;
    }
}
