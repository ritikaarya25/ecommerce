package com.acme.orders.api;

import com.acme.orders.api.ApiModels.OrderView;
import com.acme.orders.api.ApiModels.StockView;
import com.acme.orders.api.ApiModels.WarehouseView;
import com.acme.orders.inventory.InventoryService;
import com.acme.orders.inventory.WarehouseRepository;
import com.acme.orders.orders.OrderService;
import com.acme.orders.orders.OrderStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse")
@PreAuthorize("hasRole('WAREHOUSE')")
public class WarehouseController {
    private final InventoryService inventory;
    private final WarehouseRepository warehouses;
    private final OrderService orders;

    public WarehouseController(InventoryService inventory, WarehouseRepository warehouses, OrderService orders) {
        this.inventory = inventory;
        this.warehouses = warehouses;
        this.orders = orders;
    }

    @GetMapping("/warehouses")
    public List<WarehouseView> warehouses() {
        return warehouses.findAll().stream().map(warehouse -> new WarehouseView(
                warehouse.getId(), warehouse.getName(), warehouse.getLocation(), warehouse.isActive())).toList();
    }

    @GetMapping("/warehouses/{warehouseId}/stock")
    public List<StockView> stock(@PathVariable Long warehouseId) {
        return inventory.warehouseStock(warehouseId);
    }

    @PatchMapping("/orders/{orderId}/status")
    public OrderView status(@PathVariable Long orderId, @RequestBody OrderStatus status) {
        return orders.advanceStatus(orderId, status);
    }
}