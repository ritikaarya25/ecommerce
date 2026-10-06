package com.acme.orders.orders;

import com.acme.orders.common.BusinessRuleException;

public enum OrderStatus {
    PLACED, CONFIRMED, PACKED, SHIPPED, DELIVERED, RETURNED;

    public void assertTransitionTo(OrderStatus next) {
        boolean allowed = switch (this) {
            case PLACED -> next == CONFIRMED;
            case CONFIRMED -> next == PACKED;
            case PACKED -> next == SHIPPED;
            case SHIPPED -> next == DELIVERED;
            case DELIVERED -> next == RETURNED;
            case RETURNED -> false;
        };
        if (!allowed) {
            throw new BusinessRuleException("Invalid order status transition: " + this + " -> " + next);
        }
    }
}