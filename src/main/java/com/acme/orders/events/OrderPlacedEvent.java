package com.acme.orders.events;

import java.util.List;

public record OrderPlacedEvent(Long orderId, String customerUsername, List<Long> warehouseIds) {
    public OrderPlacedEvent {
        warehouseIds = List.copyOf(warehouseIds);
    }
}