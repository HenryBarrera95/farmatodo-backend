package com.farmatodo.client;

import com.farmatodo.client.dto.CreateCustomerRequest;
import com.farmatodo.log.LogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService")
class CustomerServiceTest {

    @Mock
    private CustomerRepository repo;

    @Mock
    private LogService logService;

    private CustomerService service;

    @BeforeEach
    void setUp() {
        service = new CustomerService(repo, logService);
        MDC.put("tx_id", "test-tx");
    }

    private CreateCustomerRequest validRequest() {
        var req = new CreateCustomerRequest();
        req.setName("Juan Pérez");
        req.setEmail("juan@test.com");
        req.setPhone("+573001234567");
        req.setAddress("Calle 1 #2-3");
        return req;
    }

    @Test
    @DisplayName("crea cliente cuando email y phone no existen")
    void create_success() {
        when(repo.existsByEmail("juan@test.com")).thenReturn(false);
        when(repo.existsByPhone("+573001234567")).thenReturn(false);
        when(repo.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            var saved = new Customer(c.getName(), c.getEmail(), c.getPhone(), c.getAddress(),
                    Instant.now(), "test-tx");
            com.farmatodo.TestUtils.setId(saved);
            return saved;
        });

        var result = service.create(validRequest());

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("juan@test.com");
        assertThat(result.getName()).isEqualTo("Juan Pérez");
        verify(repo).existsByEmail("juan@test.com");
        verify(repo).existsByPhone("+573001234567");
        verify(repo).save(any(Customer.class));
    }

    @Test
    @DisplayName("lanza CustomerConflictException cuando email ya existe")
    void create_throwsWhenEmailExists() {
        when(repo.existsByEmail("juan@test.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(validRequest()))
                .isInstanceOf(CustomerConflictException.class)
                .hasMessageContaining("Email already registered");

        verify(repo).existsByEmail("juan@test.com");
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("lanza CustomerConflictException cuando phone ya existe")
    void create_throwsWhenPhoneExists() {
        when(repo.existsByEmail("juan@test.com")).thenReturn(false);
        when(repo.existsByPhone("+573001234567")).thenReturn(true);

        assertThatThrownBy(() -> service.create(validRequest()))
                .isInstanceOf(CustomerConflictException.class)
                .hasMessageContaining("Phone already registered");

        verify(repo).existsByPhone("+573001234567");
        verify(repo, never()).save(any());
    }
}
