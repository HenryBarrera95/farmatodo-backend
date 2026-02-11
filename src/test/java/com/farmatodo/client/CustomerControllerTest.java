package com.farmatodo.client;

import com.farmatodo.TestUtils;
import com.farmatodo.client.dto.CreateCustomerRequest;
import com.farmatodo.client.dto.CreateCustomerResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
@DisplayName("CustomerController")
class CustomerControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CustomerService customerService;

    @Test
    @DisplayName("POST /clients retorna 201")
    void create_returns201() throws Exception {
        var customer = new Customer("Juan", "juan@test.com", "+573001234567", "Calle 1", Instant.now(), "tx");
        TestUtils.setId(customer);

        when(customerService.create(any(CreateCustomerRequest.class))).thenReturn(customer);

        mvc.perform(post("/clients")
                        .header(TestUtils.API_KEY_HEADER, TestUtils.API_KEY_DEFAULT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Juan","email":"juan@test.com","phone":"+573001234567","address":"Calle 1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/clients/")))
                .andExpect(jsonPath("$.email").value("juan@test.com"))
                .andExpect(jsonPath("$.name").value("Juan"));
    }
}
