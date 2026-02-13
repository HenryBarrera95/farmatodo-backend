package com.farmatodo.payment;

import com.farmatodo.TestUtils;
import com.farmatodo.client.Customer;
import com.farmatodo.client.CustomerRepository;
import com.farmatodo.log.LogService;
import com.farmatodo.mail.EmailService;
import com.farmatodo.order.Order;
import com.farmatodo.order.OrderNotFoundException;
import com.farmatodo.order.OrderItem;
import com.farmatodo.order.OrderRepository;
import com.farmatodo.product.Product;
import com.farmatodo.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService")
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepo;

    @Mock
    private PaymentRepository paymentRepo;

    @Mock
    private ProductRepository productRepo;

    @Mock
    private CustomerRepository customerRepo;

    @Mock
    private LogService logService;

    @Mock
    private EmailService emailService;

    private PaymentService serviceSuccess;
    private PaymentService serviceFail;
    private UUID orderId;
    private Order order;

    @BeforeEach
    void setUp() {
        MDC.put("tx_id", "test-tx");
        orderId = UUID.randomUUID();
        order = new Order(UUID.randomUUID(), UUID.randomUUID(), Order.OrderStatus.PAYMENT_PENDING,
                BigDecimal.valueOf(100), "Addr", "tok", Instant.now(), "tx");
        TestUtils.setId(order, orderId);
        var item = new OrderItem(order, UUID.randomUUID(), 2, BigDecimal.valueOf(50));
        order.getItems().add(item);

        // approveProbability=1.0 -> always approve (random > 1.0 is always false)
        serviceSuccess = new PaymentService(
                orderRepo, paymentRepo, productRepo, customerRepo,
                logService, emailService, 1.0
        );
        // approveProbability=-0.1 -> always fail (random > -0.1 is always true)
        serviceFail = new PaymentService(
                orderRepo, paymentRepo, productRepo, customerRepo,
                logService, emailService, -0.1
        );
    }

    @Test
    @DisplayName("process no hace nada cuando order no está PAYMENT_PENDING")
    void process_skipsWhenNotPending() {
        order.setStatus(Order.OrderStatus.PAID);
        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        serviceSuccess.process(orderId);

        verify(paymentRepo, never()).save(any());
        verify(orderRepo, never()).save(any());
    }

    @Test
    @DisplayName("process lanza cuando order no existe")
    void process_throwsWhenOrderNotFound() {
        when(orderRepo.findById(orderId)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> serviceSuccess.process(orderId))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    @DisplayName("process aprueba pago y actualiza stock cuando approveProbability=1")
    void process_success() {
        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepo.countByOrderId(orderId)).thenReturn(0L);
        when(paymentRepo.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        var product = new Product("P", "D", BigDecimal.valueOf(50), 10, Instant.now());
        when(productRepo.findById(any(UUID.class))).thenReturn(Optional.of(product));
        when(customerRepo.findById(any(UUID.class))).thenReturn(
                Optional.of(new Customer("N", "e@e.com", "+57", "A", Instant.now(), "tx")));

        serviceSuccess.process(orderId);

        verify(orderRepo).save(argThat(o -> o.getStatus() == Order.OrderStatus.PAID));
        verify(logService).log(eq("payment_success"), any(), any(), any());
        verify(emailService).sendPaymentSuccess(eq("e@e.com"), eq(orderId.toString()), anyString());
    }

    @Test
    @DisplayName("recover se ejecuta tras fallo y envía email")
    void recover_setsFailedAndSendsEmail() {
        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepo.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepo.findById(any(UUID.class))).thenReturn(
                Optional.of(new Customer("N", "e@e.com", "+57", "A", Instant.now(), "tx")));

        var ex = new PaymentFailedException("Payment rejected");

        serviceFail.recover(ex, orderId);

        verify(orderRepo).save(argThat(o -> o.getStatus() == Order.OrderStatus.PAYMENT_FAILED));
        verify(logService, times(2)).log(anyString(), anyString(), anyString(), any());
        verify(emailService).sendPaymentFailed(eq("e@e.com"), eq(orderId.toString()), eq("Payment rejected"));
    }
}
