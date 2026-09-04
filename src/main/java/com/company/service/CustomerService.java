package com.company.service;

import com.company.dto.CustomerRequest;
import com.company.dto.CustomerResponse;
import com.company.dto.PageResponse;
import com.company.exception.DuplicateResourceException;
import com.company.exception.NotFoundException;
import com.company.exception.ValidationException;
import com.company.model.Customer;
import com.company.model.User;
import com.company.repository.CustomerRepository;
import com.company.repository.UserRepository;
import com.company.service.EmailService;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

//imported logs

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;




import java.util.List;
import java.util.Map;







@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public CustomerService(CustomerRepository customerRepository, UserRepository userRepository, EmailService emailService) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    // Mappers
    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone()
        );
    }

    private Customer toEntity(CustomerRequest request) {
        Customer customer = new Customer();
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        return customer;
    }

    // CRUD with filters
    public PageResponse<CustomerResponse> getAllCustomers(

            int page,
            int size,
            String name,
            String email,
            String sort) {

        // Build Sort object from sort parameter
        Sort sortOrder = Sort.by("id").ascending(); // default

        if (sort != null && !sort.isEmpty()) {
            String[] sortParts = sort.split(",");
            String field = sortParts[0];
            String direction = sortParts.length > 1 ? sortParts[1] : "asc";

            Sort.Direction dir = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
            sortOrder = Sort.by(dir, field);
        }

        Pageable pageable = PageRequest.of(page, size, sortOrder);

        Page<Customer> customerPage;

        if (name != null && !name.isEmpty() && email != null && !email.isEmpty()) {
            customerPage = customerRepository.findByNameContainingIgnoreCaseAndEmailContainingIgnoreCase(name, email, pageable);
        } else if (name != null && !name.isEmpty()) {
            customerPage = customerRepository.findByNameContainingIgnoreCase(name, pageable);
        } else if (email != null && !email.isEmpty()) {
            customerPage = customerRepository.findByEmailContainingIgnoreCase(email, pageable);
        } else {
            customerPage = customerRepository.findAll(pageable);
        }

        Page<CustomerResponse> responsePage = customerPage.map(this::toResponse);

        return new PageResponse<>(
                responsePage.getContent(),
                responsePage.getNumber(),
                responsePage.getSize(),
                responsePage.getTotalElements()
        );
    }

    // flters involved
    public CustomerResponse getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer with ID " + id + " not found"));

        User currentUser = getCurrentUser();

        // If the customer has no user, only ADMIN can view it
        if (customer.getUser() == null) {
            if (!isAdmin()) {
                throw new AccessDeniedException("This customer has no owner. Only ADMIN can view it.");
            }
            return toResponse(customer);  // ADMIN can view orphaned customers
        }

        // Normal ownership check
        if (!isAdmin() && !customer.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only view your own profile");
        }

        return toResponse(customer);
    }



    //creating customers
    public CustomerResponse createCustomer(CustomerRequest request) {
        log.debug("Checking for duplicate email: {}", request.getEmail());

        User currentUser = getCurrentUser();

        //  BLOCK ADMIN from creating profiles
        if (isAdmin()) {
            throw new AccessDeniedException("ADMIN cannot create customer profiles. Only regular users can.");
        }

        //  USER can only have ONE profile
        if (customerRepository.existsByUserId(currentUser.getId())) {
            throw new ValidationException("You already have a customer profile. You cannot create another one.");
        }

        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Customer with that email already exists");
        }
        if (customerRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("Customer with that phone already exists");
        }

        Customer customer = toEntity(request);
        customer.setUser(currentUser);

        Customer saved = customerRepository.save(customer);

        emailService.sendWelcomeEmail(saved.getEmail(), saved.getName());

        log.info("Customer created with ID: {}", saved.getId());
        return toResponse(saved);
    }

    // updating with filters ...
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer with ID " + id + " not found"));

        User currentUser = getCurrentUser();

        // If customer has no user, only ADMIN can update
        if (existing.getUser() == null) {
            if (!isAdmin()) {
                throw new AccessDeniedException("This customer has no owner. Only ADMIN can update it.");
            }
            // ADMIN can update orphaned customer
        } else {
            // Normal ownership check
            if (!isAdmin() && !existing.getUser().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You can only update your own profile");
            }
        }

        if (!existing.getEmail().equalsIgnoreCase(request.getEmail())) {
            if (customerRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("You cannot update with that email because it already exists");
            }
        }

        if (!existing.getPhone().equals(request.getPhone())) {
            if (customerRepository.existsByPhone(request.getPhone())) {
                throw new DuplicateResourceException("You cannot update with that phone because it already exists");
            }
        }

        existing.setName(request.getName());
        existing.setEmail(request.getEmail());
        existing.setPhone(request.getPhone());

        Customer updated = customerRepository.save(existing);
        return toResponse(updated);
    }





    // deleting
    public boolean deleteCustomer(Long id) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer with ID " + id + " not found"));

        // Only ADMIN can delete
        if (!isAdmin()) {
            throw new AccessDeniedException("Only ADMIN can delete customers");
        }

        existing.setDeleted(true);
        customerRepository.save(existing);
        return true;
    }







    // new N+ 1 service
    public List<CustomerResponse> getAllCustomersWithAddresses() {
        List<Customer> customers = customerRepository.findAllWithAddress();
        return customers.stream()
                .map(this::toResponse)
                .collect(java.util.stream.Collectors.toList());
    }




    // PATCH
    public CustomerResponse patchCustomer(Long id, Map<String, Object> updates) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer with ID " + id + " not found"));

        User currentUser = getCurrentUser();

        // If customer has no user, only ADMIN can patch
        if (existing.getUser() == null) {
            if (!isAdmin()) {
                throw new AccessDeniedException("This customer has no owner. Only ADMIN can update it.");
            }
            // ADMIN can patch orphaned customer
        } else {
            // Normal ownership check
            if (!isAdmin() && !existing.getUser().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You can only update your own profile");
            }
        }

        if (updates.containsKey("phone")) {
            String newPhone = (String) updates.get("phone");
            if (newPhone == null || newPhone.isEmpty()) {
                throw new ValidationException("Phone cannot be null or empty");
            }
            if (!existing.getPhone().equals(newPhone)) {
                if (customerRepository.existsByPhone(newPhone)) {
                    throw new DuplicateResourceException("You cannot update with that phone because it already exists");
                }
            }
            existing.setPhone(newPhone);
        }

        if (updates.containsKey("email")) {
            String newEmail = (String) updates.get("email");
            if (newEmail == null || newEmail.isEmpty()) {
                throw new ValidationException("Email cannot be null or empty");
            }
            if (!isValidEmail(newEmail)) {
                throw new ValidationException("Email must be valid");
            }
            if (!existing.getEmail().equalsIgnoreCase(newEmail)) {
                if (customerRepository.existsByEmail(newEmail)) {
                    throw new DuplicateResourceException("You cannot update with that email because it already exists");
                }
            }
            existing.setEmail(newEmail);
        }

        if (updates.containsKey("name")) {
            String newName = (String) updates.get("name");
            if (newName == null || newName.isEmpty()) {
                throw new ValidationException("Name cannot be null or empty");
            }
            existing.setName(newName);
        }

        Customer updated = customerRepository.save(existing);
        return toResponse(updated);
    }



    // Helpers
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    // NEW: Get current logged-in user
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    // NEW: Check if current user is ADMIN
    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }



}