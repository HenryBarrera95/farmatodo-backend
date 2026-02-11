package com.farmatodo.order;

import com.farmatodo.TestUtils;
import com.farmatodo.cart.Cart;
import com.farmatodo.cart.CartItem;
import com.farmatodo.cart.CartRepository;
import com.farmatodo.log.LogService;
import com.farmatodo.order.dto.CreateOrderRequest;
import com.farmatodo.payment.PaymentService;
import com.farmatodo.product.Product;
import com.farmatodo.product.ProductRepository;
import com.farmatodo.token.CardTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepo;

    @Mock
    private OrderItemRepository orderItemRepo;

    @Mock
    private CartRepository cartRepo;

    @Mock
    private ProductRepository productRepo;

    @Mock
    private CardTokenRepository tokenRepo;

    @Mock
    private LogService logService;

    @Mock
    private PaymentService paymentService;

    private OrderService service;
    private UUID customerId;
    private UUID cartId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        MDC.put("tx_id", "test-tx");
        customerId = UUID.randomUUID();
        cartId = UUID.randomUUID();
        productId = UUID.randomUUID();

        OrderService realService = new OrderService(
                orderRepo, orderItemRepo, cartRepo, productRepo,
                tokenRepo, logService, paymentService, null
        );
        service = new OrderService(
                orderRepo, orderItemRepo, cartRepo, productRepo,
                tokenRepo, logService, paymentService, realService
        );
    }

    private CreateOrderRequest validRequest(String tokenId) {
        var req = new CreateOrderRequest();
        req.setCustomerId(customerId);
        req.setDeliveryAddress("Calle 1 #2-3");
        req.setToken(tokenId);
        return req;
    }

    @Test
    @DisplayName("createOrderAndCart lanza OrderException cuando no hay carrito activo")
    void createOrderAndCart_throwsWhenNoActiveCart() {
        when(cartRepo.findByCustomerIdAndStatus(customerId, Cart.CartStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createOrderAndCart(validRequest("token-123")))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("No active cart");
        verify(tokenRepo, never()).existsById(any());
    }

    @Test
    @DisplayName("createOrderAndCart lanza OrderException cuando carrito está vacío")
    void createOrderAndCart_throwsWhenCartEmpty() {
        var cart = new Cart(customerId, Cart.CartStatus.ACTIVE, Instant.now());
        TestUtils.setId(cart, cartId);
        when(cartRepo.findByCustomerIdAndStatus(customerId, Cart.CartStatus.ACTIVE))
                .thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> service.createOrderAndCart(validRequest("token-123")))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("Cart is empty");
    }

    @Test
    @DisplayName("createOrderAndCart lanza OrderException cuando token inválido")
    void createOrderAndCart_throwsWhenInvalidToken() {
        var cart = new Cart(customerId, Cart.CartStatus.ACTIVE, Instant.now());
        TestUtils.setId(cart, cartId);
        var item = new CartItem(cart, productId, 1, BigDecimal.TEN);
        cart.getItems().add(item);

        when(cartRepo.findByCustomerIdAndStatus(customerId, Cart.CartStatus.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(tokenRepo.existsById("token-123")).thenReturn(false);

        assertThatThrownBy(() -> service.createOrderAndCart(validRequest("token-123")))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("Invalid token");
    }

    @Test
    @DisplayName("createOrderAndCart lanza OrderException cuando producto no existe")
    void createOrderAndCart_throwsWhenProductNotFound() {
        var cart = new Cart(customerId, Cart.CartStatus.ACTIVE, Instant.now());
        TestUtils.setId(cart, cartId);
        var item = new CartItem(cart, productId, 1, BigDecimal.TEN);
        cart.getItems().add(item);

        when(cartRepo.findByCustomerIdAndStatus(customerId, Cart.CartStatus.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(tokenRepo.existsById("token-123")).thenReturn(true);
        when(productRepo.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createOrderAndCart(validRequest("token-123")))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("createOrderAndCart lanza OrderException cuando stock insuficiente")
    void createOrderAndCart_throwsWhenInsufficientStock() {
        var cart = new Cart(customerId, Cart.CartStatus.ACTIVE, Instant.now());
        TestUtils.setId(cart, cartId);
        var item = new CartItem(cart, productId, 10, BigDecimal.TEN);
        cart.getItems().add(item);

        var product = new Product("Prod", "Desc", BigDecimal.TEN, 5, Instant.now());
        when(cartRepo.findByCustomerIdAndStatus(customerId, Cart.CartStatus.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(tokenRepo.existsById("token-123")).thenReturn(true);
        when(productRepo.findById(productId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.createOrderAndCart(validRequest("token-123")))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("createOrderAndCart crea orden y items correctamente")
    void createOrderAndCart_success() {
        var cart = new Cart(customerId, Cart.CartStatus.ACTIVE, Instant.now());
        TestUtils.setId(cart, cartId);
        var item = new CartItem(cart, productId, 2, BigDecimal.valueOf(100));
        cart.getItems().add(item);

        var product = new Product("Prod", "Desc", BigDecimal.valueOf(100), 10, Instant.now());
        when(cartRepo.findByCustomerIdAndStatus(customerId, Cart.CartStatus.ACTIVE))
                .thenReturn(Optional.of(cart));
        when(tokenRepo.existsById("token-123")).thenReturn(true);
        when(productRepo.findById(productId)).thenReturn(Optional.of(product));

        var orderId = UUID.randomUUID();
        when(orderRepo.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            TestUtils.setId(o, orderId);
            return o;
        });
        when(orderItemRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.createOrderAndCart(validRequest("  token-123  "));

        assertThat(result).isNotNull();
        assertThat(result.getCustomerId()).isEqualTo(customerId);
        assertThat(result.getCartId()).isEqualTo(cartId);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("200");
        assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.PAYMENT_PENDING);
        assertThat(result.getTokenId()).isEqualTo("token-123");
        assertThat(result.getItems()).hasSize(1);

        verify(cartRepo).save(argThat(c -> c.getStatus() == Cart.CartStatus.ORDERED));
        verify(logService).log(eq("order_created"), any(), any(), any(Map.class));
    }

    @Test
    @DisplayName("toResponse mapea Order a OrderResponse")
    void toResponse_mapsCorrectly() {
        var orderId = UUID.randomUUID();
        var order = new Order(customerId, cartId, Order.OrderStatus.PAID,
                BigDecimal.valueOf(200), "Calle 1", "tok", Instant.now(), "tx");
        TestUtils.setId(order, orderId);
        var oi = new OrderItem(order, productId, 2, BigDecimal.valueOf(100));
        order.getItems().add(oi);

        var response = service.toResponse(order);

        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.status()).isEqualTo(Order.OrderStatus.PAID);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(2);
        assertThat(response.items().get(0).subtotal()).isEqualByComparingTo("200");
    }
}
