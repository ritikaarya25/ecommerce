package com.acme.orders.api;

import com.acme.orders.api.ApiModels.OperationalRecordView;
import com.acme.orders.events.OperationalRecordRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class OperationsController {
    private final OperationalRecordRepository records;

    public OperationsController(OperationalRecordRepository records) {
        this.records = records;
    }

    @GetMapping("/{orderId}/operations")
    public List<OperationalRecordView> records(@PathVariable Long orderId) {
        return records.findByOrderIdOrderByCreatedAtAsc(orderId).stream().map(record -> new OperationalRecordView(
                record.getId(), record.getKind(), record.getOrderId(), record.getDetail(), record.getCreatedAt())).toList();
    }
}