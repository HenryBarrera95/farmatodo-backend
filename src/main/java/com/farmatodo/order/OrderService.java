package com.farmatodo.order;

import com.farmatodo.cart.Cart;
import com.farmatodo.cart.CartRepository;
import com.farmatodo.config.TxFilter;
import com.farmatodo.log.LogService;
import com.farmatodo.order.dto.CreateOrderRequest;
import com.farmatodo.order.dto.OrderItemResponse;
import com.farmatodo.order.dto.OrderResponse;
import com.farmatodo.payment.PaymentService;
import com.farmatodo.product.Product;
import com.farmatodo.product.ProductRepository;
import com.farmatodo.token.CardTokenRepository;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final CartRepository cartRepo;
    private final ProductRepository productRepo;
    private final CardTokenRepository tokenRepo;
    private final LogService logService;
    private final PaymentService paymentService;
    private final OrderService self;

    public OrderService(OrderRepository orderRepo,
                        OrderItemRepository orderItemRepo,
                        CartRepository cartRepo,
                        ProductRepository productRepo,
                        CardTokenRepository tokenRepo,
                        LogService logService,
                        PaymentService paymentService,
                        @Lazy OrderService self) {
        this.orderRepo = orderRepo;
        this.orderItemRepo = orderItemRepo;
        this.cartRepo = cartRepo;
        this.productRepo = productRepo;
        this.tokenRepo = tokenRepo;
        this.logService = logService;
        this.paymentService = paymentService;
        this.self = self;
    }

    @Transactional
    public Order create(CreateOrderRequest req) {
        Order order = self.createOrderAndCart(req);
        paymentService.process(order.getId());
        return orderRepo.findById(order.getId()).orElse(order);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order createOrderAndCart(CreateOrderRequest req) {
        String tx = MDC.get(TxFilter.TX_ID);

        Cart cart = cartRepo.findByCustomerIdAndStatus(req.getCustomerId(), Cart.CartStatus.ACTIVE)
                .orElseThrow(() -> new OrderException("No active cart for customer"));

        if (cart.getItems().isEmpty()) {
            throw new OrderException("Cart is empty");
        }

        if (!tokenRepo.existsById(req.getToken().trim())) {
            throw new OrderException("Invalid token");
        }

        for (var cartItem : cart.getItems()) {
            Product product = productRepo.findById(cartItem.getProductId())
                    .orElseThrow(() -> new OrderException("Product not found: " + cartItem.getProductId()));
            if (product.getStock() < cartItem.getQuantity()) {
                throw new OrderException(
                        "Insufficient stock for product " + product.getName() + ": required " + cartItem.getQuantity()
                                + ", available " + product.getStock());
            }
        }

        BigDecimal total = cart.getItems().stream()
                .map(i -> i.getUnitPriceSnapshot().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order(
                req.getCustomerId(),
                cart.getId(),
                Order.OrderStatus.PAYMENT_PENDING,
                total,
                req.getDeliveryAddress().trim(),
                req.getToken().trim(),
                Instant.now(),
                tx
        );
        order = orderRepo.save(order);

        for (var cartItem : cart.getItems()) {
            var orderItem = new OrderItem(
                    order,
                    cartItem.getProductId(),
                    cartItem.getQuantity(),
                    cartItem.getUnitPriceSnapshot()
            );
            orderItemRepo.save(orderItem);
            order.getItems().add(orderItem);
        }

        cart.setStatus(Cart.CartStatus.ORDERED);
        cartRepo.save(cart);

        logService.log("order_created", "INFO", "Order created, payment pending",
                Map.of("orderId", order.getId().toString(), "customerId", req.getCustomerId().toString(),
                        "total", total.toString(), "tokenId", req.getToken()));
        log.info("Order created {} [tx={}]", order.getId(), tx);

        return order;
    }

    public OrderResponse toResponse(Order order) {
        var items = order.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getProductId(),
                        i.getQuantity(),
                        i.getUnitPriceSnapshot(),
                        i.getSubtotal()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getCartId(),
                order.getStatus(),
                items,
                order.getTotalAmount(),
                order.getDeliveryAddress(),
                order.getTokenId()
        );
    }
}
