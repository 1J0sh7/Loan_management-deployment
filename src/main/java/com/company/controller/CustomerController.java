package com.company.controller;

import com.company.dto.CustomerRequest;
import com.company.dto.CustomerResponse;
import com.company.dto.PageResponse;
import com.company.dto.ErrorResponse;


import com.company.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;


//importing logs

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//importing swagger
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;




import java.util.List;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers", description = "Customer management endpoints")

// the main class
public class CustomerController {
    //Logger class

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);


    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    //pagination
    @GetMapping
    @Operation(summary = "List customers (paginated)", description = "Returns a page of customers, optionally filtered by name or email and sorted.")
    public PageResponse<CustomerResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String sort) {

        return customerService.getAllCustomers(page, size, name, email, sort);
    }

    // searching customerwith Id
    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID", description = "Returns a single customer, or 404 if no customer exists with the given ID.")
    public ResponseEntity<?> getOne(@PathVariable Long id) {
        CustomerResponse customer = customerService.getCustomerById(id);
        if (customer == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(404, "Customer with ID " + id + " not found"));
        }
        return ResponseEntity.ok(customer);
    }

    // Creating a customer
    @PostMapping
    @Operation(summary = "Create customer", description = "Creates a new customer record from the given request body.")
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        // adding simple log
        log.info("Received request to create customer with email: {}", request.getEmail());

        CustomerResponse created = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    //Updatting a customer.
    @PutMapping("/{id}")
    @Operation(summary = "Update customer", description = "Replaces the full customer record for the given ID, or 404 if not found.")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        CustomerResponse updated = customerService.updateCustomer(id, request);
        if (updated == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(404, "Customer with ID " + id + " not found"));
        }
        return ResponseEntity.ok(updated);
    }

    // Updating customer by PATCHING ONLY 1 entity or field  EDITED
    @PatchMapping("/{id}")
    @Operation(summary = "Partially update customer", description = "Updates only the fields provided in the request body, or 404 if the customer is not found.")
    public ResponseEntity<?> patch(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        CustomerResponse updated = customerService.patchCustomer(id, updates);
        if (updated == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(404, "Customer with ID " + id + " not found"));
        }
        return ResponseEntity.ok(updated);
    }


    // n + 1 fix
    @GetMapping("/with-addresses")
    @Operation(summary = "List customers with addresses", description = "Returns all customers with their addresses eagerly loaded, avoiding N+1 query issues.")
    public List<CustomerResponse> getAllWithAddresses() {
        return customerService.getAllCustomersWithAddresses();
    }










    // Deleting a Customer
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete customer", description = "Deletes the customer with the given ID, or 404 if no such customer exists.")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        boolean deleted = customerService.deleteCustomer(id);
        if (!deleted) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(404, "Customer with ID " + id + " not found"));
        }
        return ResponseEntity.noContent().build();
    }

    // Handle validation of  errors manually
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return errors;
    }
}