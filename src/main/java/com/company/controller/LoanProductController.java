package com.company.controller;

import com.company.dto.LoanProductResponseDTO;
import com.company.mapper.LoanMapper;
import com.company.model.LoanProduct;
import com.company.service.LoanProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/loan-products")
@Tag(name = "Loan Products", description = "Loan product management endpoints")
public class LoanProductController {

    private final LoanProductService loanProductService;

    public LoanProductController(LoanProductService loanProductService) {
        this.loanProductService = loanProductService;
    }

    // 1. GET all loan products (Paginated)
    @GetMapping
    @Operation(summary = "[USER] List loan products", description = "Returns paginated list of all loan products.")
    public ResponseEntity<Page<LoanProductResponseDTO>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        // ✅ This returns a real Page<LoanProduct> from the database
        Page<LoanProduct> productPage = loanProductService.getAllProducts(pageable);

        // ✅ Map to DTO
        Page<LoanProductResponseDTO> dtoPage = productPage.map(LoanMapper::toLoanProductResponseDTO);

        return ResponseEntity.ok(dtoPage);
    }

    // 2. GET loan product by ID
    @GetMapping("/{id}")
    @Operation(summary = "[USER] Get loan product by ID", description = "Returns a single loan product.")
    public ResponseEntity<LoanProductResponseDTO> getProductById(@PathVariable Long id) {
        LoanProduct product = loanProductService.getLoanProductById(id);
        return ResponseEntity.ok(LoanMapper.toLoanProductResponseDTO(product));
    }

    // 3. POST create loan product (ADMIN only)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Create a loan product", description = "Creates a new loan product.")
    public ResponseEntity<LoanProduct> createLoanProduct(@RequestBody LoanProduct product) {
        return ResponseEntity.ok(loanProductService.createLoanProduct(product));
    }

    // 4. PUT update loan product (ADMIN only)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Update a loan product", description = "Updates an existing loan product.")
    public ResponseEntity<LoanProduct> updateLoanProduct(@PathVariable Long id, @RequestBody LoanProduct product) {
        return ResponseEntity.ok(loanProductService.updateLoanProduct(id, product));
    }

    // 5. DELETE loan product (ADMIN only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Delete a loan product", description = "Soft deletes a loan product.")
    public ResponseEntity<Void> deleteLoanProduct(@PathVariable Long id) {
        loanProductService.deleteLoanProduct(id);
        return ResponseEntity.noContent().build();
    }
}