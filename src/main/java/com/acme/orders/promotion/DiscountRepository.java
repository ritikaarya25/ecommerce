package com.acme.orders.promotion;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DiscountRepository extends JpaRepository<Discount, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Discount d where upper(d.code) = upper(:code)")
    Optional<Discount> lockByCode(@Param("code") String code);
}