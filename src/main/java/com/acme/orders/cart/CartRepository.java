package com.acme.orders.cart;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<ShoppingCart, Long> {
    Optional<ShoppingCart> findByOwner(String owner);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distinct c from ShoppingCart c left join fetch c.items i left join fetch i.product where c.owner = :owner")
    Optional<ShoppingCart> lockByOwner(@Param("owner") String owner);
}