package com.acme.orders.api;

import com.acme.orders.api.ApiModels.CheckoutRequest;
import com.acme.orders.api.ApiModels.OrderView;
import com.acme.orders.api.ApiModels.ReturnRequestBody;
import com.acme.orders.api.ApiModels.ReturnView;
import com.acme.orders.orders.OrderService;
import com.acme.orders.returns.ReturnService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasRole('CUSTOMER')")
public class OrderController {
    private final OrderService orders;
    private final ReturnService returns;

    public OrderController(OrderService orders, ReturnService returns) {
        this.orders = orders;
        this.returns = returns;
    }

    @PostMapping("/checkout")
    public OrderView checkout(Authentication authentication, @Valid @RequestBody CheckoutRequest request) {
        return orders.checkout(authentication.getName(), request);
    }

    @GetMapping
    public List<OrderView> orders(Authentication authentication) {
        return orders.customerOrders(authentication.getName());
    }

    @GetMapping("/{orderId}")
    public OrderView order(Authentication authentication, @PathVariable Long orderId) {
        return orders.customerOrder(authentication.getName(), orderId);
    }

    @PostMapping("/{orderId}/returns")
    public ReturnView requestReturn(Authentication authentication, @PathVariable Long orderId,
                                    @Valid @RequestBody ReturnRequestBody request) {
        return returns.request(authentication.getName(), orderId, request);
    }
}