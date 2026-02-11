package com.farmatodo.cart;

import com.farmatodo.cart.dto.AddCartItemRequest;
import com.farmatodo.cart.dto.CartItemResponse;
import com.farmatodo.cart.dto.CartResponse;
import com.farmatodo.client.CustomerRepository;
import com.farmatodo.product.Product;
import com.farmatodo.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CartService {

    private final CartRepository cartRepo;
    private final CartItemRepository cartItemRepo;
    private final ProductRepository productRepo;
    private final CustomerRepository customerRepo;

    public CartService(CartRepository cartRepo,
                       CartItemRepository cartItemRepo,
                       ProductRepository productRepo,
                       CustomerRepository customerRepo) {
        this.cartRepo = cartRepo;
        this.cartItemRepo = cartItemRepo;
        this.productRepo = productRepo;
        this.customerRepo = customerRepo;
    }

    /**
     * Light validation at add: product exists, stock available.
     * Definitive stock validation happens at order creation.
     */
    @Transactional
    public Cart addItem(AddCartItemRequest req) {
        if (!customerRepo.existsById(req.getCustomerId())) {
            throw new CartException("Customer not found");
        }

        Product product = productRepo.findById(req.getProductId())
                .orElseThrow(() -> new CartException("Product not found"));

        if (product.getStock() < 1) {
            throw new CartException("Product out of stock");
        }

        Cart cart = cartRepo.findByCustomerIdAndStatus(req.getCustomerId(), Cart.CartStatus.ACTIVE)
                .orElseGet(() -> cartRepo.save(new Cart(req.getCustomerId(), Cart.CartStatus.ACTIVE, Instant.now())));

        var existing = cartItemRepo.findByCartIdAndProductId(cart.getId(), req.getProductId());

        if (existing.isPresent()) {
            var item = existing.get();
            int newQty = item.getQuantity() + req.getQuantity();
            if (product.getStock() < newQty) {
                throw new CartException("Insufficient stock: available " + product.getStock());
            }
            item.setQuantity(newQty);
            item.setUnitPriceSnapshot(product.getPrice());
            cartItemRepo.save(item);
        } else {
            if (product.getStock() < req.getQuantity()) {
                throw new CartException("Insufficient stock: available " + product.getStock());
            }
            var item = new CartItem(cart, req.getProductId(), req.getQuantity(), product.getPrice());
            cartItemRepo.save(item);
        }

        return cartRepo.findById(cart.getId()).orElse(cart);
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(UUID customerId) {
        Cart cart = cartRepo.findByCustomerIdAndStatus(customerId, Cart.CartStatus.ACTIVE)
                .orElseThrow(() -> new CartException("No active cart for customer"));

        var items = cart.getItems().stream()
                .map(i -> new CartItemResponse(
                        i.getProductId(),
                        i.getQuantity(),
                        i.getUnitPriceSnapshot(),
                        i.getSubtotal()
                ))
                .toList();

        BigDecimal total = cart.getItems().stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(cart.getId(), cart.getCustomerId(), items, total);
    }
}
