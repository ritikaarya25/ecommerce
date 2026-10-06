package com.acme.orders.returns;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    Optional<ReturnRequest> findByOrderId(Long orderId);
}