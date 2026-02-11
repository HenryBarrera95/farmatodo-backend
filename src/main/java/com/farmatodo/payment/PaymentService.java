package com.farmatodo.payment;

import com.farmatodo.client.Customer;
import com.farmatodo.client.CustomerRepository;
import com.farmatodo.config.TxFilter;
import com.farmatodo.log.LogService;
import com.farmatodo.mail.EmailService;
import com.farmatodo.order.Order;
import com.farmatodo.order.OrderRepository;
import com.farmatodo.product.Product;
import com.farmatodo.product.ProductRepository;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final OrderRepository orderRepo;
    private final PaymentRepository paymentRepo;
    private final ProductRepository productRepo;
    private final CustomerRepository customerRepo;
    private final LogService logService;
    private final EmailService emailService;

    private final double approveProbability;

    public PaymentService(OrderRepository orderRepo,
                          PaymentRepository paymentRepo,
                          ProductRepository productRepo,
                          CustomerRepository customerRepo,
                          LogService logService,
                          EmailService emailService,
                          @Value("${payment.approve-probability:0.7}") double approveProbability) {
        this.orderRepo = orderRepo;
        this.paymentRepo = paymentRepo;
        this.productRepo = productRepo;
        this.customerRepo = customerRepo;
        this.logService = logService;
        this.emailService = emailService;
        this.approveProbability = approveProbability;
    }

    @Retryable(
            value = PaymentFailedException.class,
            maxAttemptsExpression = "${payment.retry.max-attempts:3}",
            backoff = @Backoff(
                    delayExpression = "${payment.retry.delay:1000}",
                    multiplierExpression = "${payment.retry.multiplier:2}"
            )
    )
    @Transactional
    public void process(UUID orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() != Order.OrderStatus.PAYMENT_PENDING) {
            return;
        }

        String tx = MDC.get(TxFilter.TX_ID);
        int attempts = (int) paymentRepo.countByOrderId(orderId) + 1;

        Payment payment = new Payment(
                orderId,
                order.getTokenId(),
                Payment.PaymentStatus.INITIATED,
                attempts,
                null,
                Instant.now(),
                tx
        );
        payment = paymentRepo.save(payment);

        double random = ThreadLocalRandom.current().nextDouble();
        if (random > approveProbability) {
            String errorMsg = "Payment rejected by simulator (attempt " + attempts + ")";
            payment.setLastError(errorMsg);
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepo.save(payment);
            log.warn("Payment attempt {} failed for order {} [tx={}]", attempts, orderId, tx);
            throw new PaymentFailedException(errorMsg);
        }

        order.setStatus(Order.OrderStatus.PAID);
        orderRepo.save(order);

        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        paymentRepo.save(payment);

        for (var item : order.getItems()) {
            Product product = productRepo.findById(item.getProductId()).orElseThrow();
            product.setStock(product.getStock() - item.getQuantity());
            productRepo.save(product);
        }

        logService.log("payment_success", "INFO", "Payment successful",
                Map.of("orderId", orderId.toString(), "attempts", attempts));
        log.info("Payment successful for order {} [tx={}]", orderId, tx);

        Customer customer = customerRepo.findById(order.getCustomerId()).orElse(null);
        if (customer != null) {
            emailService.sendPaymentSuccess(customer.getEmail(), orderId.toString(), order.getTotalAmount().toString());
        }
    }

    @Recover
    @Transactional
    public void recover(PaymentFailedException ex, UUID orderId) {
        String tx = MDC.get(TxFilter.TX_ID);
        log.warn("All payment retries exhausted for order {} [tx={}]", orderId, tx);

        Order order = orderRepo.findById(orderId).orElseThrow();
        order.setStatus(Order.OrderStatus.PAYMENT_FAILED);
        orderRepo.save(order);

        var payment = new Payment(orderId, order.getTokenId(), Payment.PaymentStatus.FAILED,
                0, ex.getMessage(), Instant.now(), tx);
        paymentRepo.save(payment);

        logService.log("payment_failed", "WARN", "Payment failed after all retries",
                Map.of("orderId", orderId.toString(), "error", ex.getMessage()));
        logService.log("email_sent_payment_failed", "INFO", "Failure notification sent",
                Map.of("orderId", orderId.toString()));

        Customer customer = customerRepo.findById(order.getCustomerId()).orElse(null);
        if (customer != null) {
            emailService.sendPaymentFailed(customer.getEmail(), orderId.toString(), ex.getMessage());
        }
    }
}
