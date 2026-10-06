package com.acme.orders.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockRepository extends JpaRepository<Stock, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s join fetch s.warehouse where s.product.id = :productId and s.warehouse.active = true order by s.warehouse.id")
    List<Stock> lockByProductId(@Param("productId") Long productId);

    List<Stock> findByWarehouseIdOrderByProductId(Long warehouseId);
    java.util.Optional<Stock> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.product.id = :productId and s.warehouse.id = :warehouseId")
    java.util.Optional<Stock> lockByProductAndWarehouse(@Param("productId") Long productId,
                                                        @Param("warehouseId") Long warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.id in :ids order by s.id")
    List<Stock> lockByIds(@Param("ids") List<Long> ids);
}