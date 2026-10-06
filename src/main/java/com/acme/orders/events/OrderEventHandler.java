package com.acme.orders.events;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
@Component
public class OrderEventHandler {
    private final OperationalRecordRepository records;

    public OrderEventHandler(OperationalRecordRepository records) {
        this.records = records;
    }

    @Async("orderEventsExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        save("FULFILLMENT_ROUTING", event.orderId(), "Routed to warehouses " + event.warehouseIds());
        save("CUSTOMER_NOTIFICATION", event.orderId(), "Order confirmation sent to " + event.customerUsername());
        save("AUDIT", event.orderId(), "Order placed");
    }

    @Async("orderEventsExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener
    public void onOrderLifecycleChanged(OrderLifecycleEvent event) {
        save("CUSTOMER_NOTIFICATION", event.orderId(), "Order status changed to " + event.status());
        save("AUDIT", event.orderId(), "Order status changed to " + event.status());
    }

    private void save(String kind, Long orderId, String detail) {
        OperationalRecord record = new OperationalRecord();
        record.setKind(kind);
        record.setOrderId(orderId);
        record.setDetail(detail);
        record.setCreatedAt(Instant.now());
        records.save(record);
    }
}