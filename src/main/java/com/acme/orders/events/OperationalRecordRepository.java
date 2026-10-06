package com.acme.orders.events;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperationalRecordRepository extends JpaRepository<OperationalRecord, Long> {
    List<OperationalRecord> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}