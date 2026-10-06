package com.acme.orders.orders;

import com.acme.orders.common.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderStatusTest {
    @Test
    void acceptsOnlyTheNextFulfillmentState() {
        assertDoesNotThrow(() -> OrderStatus.PLACED.assertTransitionTo(OrderStatus.CONFIRMED));
        assertDoesNotThrow(() -> OrderStatus.DELIVERED.assertTransitionTo(OrderStatus.RETURNED));
        assertThrows(BusinessRuleException.class, () -> OrderStatus.PLACED.assertTransitionTo(OrderStatus.SHIPPED));
        assertThrows(BusinessRuleException.class, () -> OrderStatus.RETURNED.assertTransitionTo(OrderStatus.DELIVERED));
    }
}