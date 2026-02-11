package com.farmatodo.client;

import com.farmatodo.client.dto.CreateCustomerRequest;
import com.farmatodo.client.dto.CreateCustomerResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/clients")
@Validated
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CreateCustomerResponse> create(@Valid @RequestBody CreateCustomerRequest req) {
        var customer = customerService.create(req);
        var location = URI.create("/clients/" + customer.getId());
        return ResponseEntity.created(location).body(new CreateCustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getAddress()
        ));
    }
}
