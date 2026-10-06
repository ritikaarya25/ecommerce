package com.acme.orders.api;

import com.acme.orders.api.ApiModels.CartItemRequest;
import com.acme.orders.api.ApiModels.CartView;
import com.acme.orders.cart.CartService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {
    private final CartService carts;

    public CartController(CartService carts) {
        this.carts = carts;
    }

    @GetMapping
    public CartView get(Authentication authentication) {
        return carts.get(authentication.getName());
    }

    @PostMapping("/items")
    public CartView add(Authentication authentication, @Valid @RequestBody CartItemRequest request) {
        return carts.add(authentication.getName(), request.productId(), request.quantity());
    }

    @DeleteMapping("/items/{productId}")
    public CartView remove(Authentication authentication, @PathVariable Long productId) {
        return carts.remove(authentication.getName(), productId);
    }
}