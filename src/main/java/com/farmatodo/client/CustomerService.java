package com.farmatodo.client;

import com.farmatodo.client.dto.CreateCustomerRequest;
import com.farmatodo.config.TxFilter;
import com.farmatodo.log.LogService;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository repo;
    private final LogService logService;

    public CustomerService(CustomerRepository repo, LogService logService) {
        this.repo = repo;
        this.logService = logService;
    }

    @Transactional
    public Customer create(CreateCustomerRequest req) {
        String tx = MDC.get(TxFilter.TX_ID);

        String email = req.getEmail().trim().toLowerCase();
        String phone = req.getPhone().trim();

        if (repo.existsByEmail(email)) {
            logService.log("customer_conflict", "WARN", "Email already registered",
                    Map.of("field", "email", "email", email));
            throw new CustomerConflictException("Email already registered");
        }
        if (repo.existsByPhone(phone)) {
            logService.log("customer_conflict", "WARN", "Phone already registered",
                    Map.of("field", "phone", "phone", phone));
            throw new CustomerConflictException("Phone already registered");
        }

        Customer customer = new Customer(
                req.getName().trim(),
                email,
                phone,
                req.getAddress().trim(),
                Instant.now(),
                tx
        );

        customer = repo.save(customer);

        logService.log("customer_created", "INFO", "Customer registered successfully",
                Map.of("customerId", customer.getId().toString(), "email", customer.getEmail()));
        log.info("Customer created {} [tx={}]", customer.getId(), tx);

        return customer;
    }
}
