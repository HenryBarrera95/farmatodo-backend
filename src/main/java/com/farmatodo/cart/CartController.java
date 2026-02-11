package com.farmatodo.cart;

import com.farmatodo.cart.dto.AddCartItemRequest;
import com.farmatodo.cart.dto.CartResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/carts")
@Validated
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@Valid @RequestBody AddCartItemRequest req) {
        var cart = cartService.addItem(req);
        var response = cartService.getCart(req.getCustomerId());
        return ResponseEntity.created(URI.create("/carts/" + cart.getId())).body(response);
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@RequestParam UUID customerId) {
        var cart = cartService.getCart(customerId);
        return ResponseEntity.ok(cart);
    }
}
