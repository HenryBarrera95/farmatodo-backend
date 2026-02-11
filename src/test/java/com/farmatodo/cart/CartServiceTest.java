package com.farmatodo.cart;

import com.farmatodo.TestUtils;
import com.farmatodo.cart.dto.AddCartItemRequest;
import com.farmatodo.client.CustomerRepository;
import com.farmatodo.product.Product;
import com.farmatodo.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService")
class CartServiceTest {

    @Mock
    private CartRepository cartRepo;

    @Mock
    private CartItemRepository cartItemRepo;

    @Mock
    private ProductRepository productRepo;

    @Mock
    private CustomerRepository customerRepo;

    private CartService service;

    private final UUID customerId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CartService(cartRepo, cartItemRepo, productRepo, customerRepo);
    }

    @Test
    @DisplayName("addItem lanza CartException cuando customer no existe")
    void addItem_throwsWhenCustomerNotFound() {
        when(customerRepo.existsById(customerId)).thenReturn(false);

        var req = new AddCartItemRequest();
        req.setCustomerId(customerId);
        req.setProductId(productId);
        req.setQuantity(1);

        assertThatThrownBy(() -> service.addItem(req))
                .isInstanceOf(CartException.class)
                .hasMessageContaining("Customer not found");
    }

    @Test
    @DisplayName("addItem lanza CartException cuando product no existe")
    void addItem_throwsWhenProductNotFound() {
        when(customerRepo.existsById(customerId)).thenReturn(true);
        when(productRepo.findById(productId)).thenReturn(Optional.empty());

        var req = new AddCartItemRequest();
        req.setCustomerId(customerId);
        req.setProductId(productId);
        req.setQuantity(1);

        assertThatThrownBy(() -> service.addItem(req))
                .isInstanceOf(CartException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("addItem crea nuevo cart e item cuando no hay cart activo")
    void addItem_createsNewCartAndItem() {
        var product = new Product("Prod", "Desc", BigDecimal.valueOf(100), 10, Instant.now());
        when(customerRepo.existsById(customerId)).thenReturn(true);
        when(productRepo.findById(productId)).thenReturn(Optional.of(product));
        when(cartRepo.findByCustomerIdAndStatus(customerId, Cart.CartStatus.ACTIVE)).thenReturn(Optional.empty());
        when(cartItemRepo.findByCartIdAndProductId(any(), eq(productId))).thenReturn(Optional.empty());

        var newCart = new Cart(customerId, Cart.CartStatus.ACTIVE, Instant.now());
        var savedCartId = UUID.randomUUID();
        when(cartRepo.save(any(Cart.class))).thenAnswer(inv -> {
            Cart c = inv.getArgument(0);
            TestUtils.setId(c, savedCartId);
            return c;
        });
        when(cartRepo.findById(any(UUID.class))).thenAnswer(inv -> {
            Cart c = new Cart(customerId, Cart.CartStatus.ACTIVE, Instant.now());
            TestUtils.setId(c, savedCartId);
            return Optional.of(c);
        });
        when(cartItemRepo.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        var req = new AddCartItemRequest();
        req.setCustomerId(customerId);
        req.setProductId(productId);
        req.setQuantity(2);

        var result = service.addItem(req);

        assertThat(result).isNotNull();
        verify(cartRepo).save(any(Cart.class));
        verify(cartItemRepo).save(any(CartItem.class));
    }

    @Test
    @DisplayName("getCart lanza CartException cuando no hay carrito activo")
    void getCart_throwsWhenNoActiveCart() {
        when(cartRepo.findByCustomerIdAndStatus(customerId, Cart.CartStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCart(customerId))
                .isInstanceOf(CartException.class)
                .hasMessageContaining("No active cart");
    }

    @Test
    @DisplayName("getCart devuelve CartResponse con items y total")
    void getCart_returnsCartResponse() {
        var cart = new Cart(customerId, Cart.CartStatus.ACTIVE, Instant.now());
        var cartId = UUID.randomUUID();
        TestUtils.setId(cart, cartId);
        var item = new CartItem(cart, productId, 2, BigDecimal.valueOf(100));
        cart.getItems().add(item);

        when(cartRepo.findByCustomerIdAndStatus(customerId, Cart.CartStatus.ACTIVE))
                .thenReturn(Optional.of(cart));

        var result = service.getCart(customerId);

        assertThat(result).isNotNull();
        assertThat(result.customerId()).isEqualTo(customerId);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).quantity()).isEqualTo(2);
        assertThat(result.total()).isEqualByComparingTo("200");
    }
}
