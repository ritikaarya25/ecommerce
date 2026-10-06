package com.acme.orders.events;

public record OrderLifecycleEvent(Long orderId, String status) {
}