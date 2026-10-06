package com.acme.orders.api;

import com.acme.orders.api.ApiModels.CategoryRequest;
import com.acme.orders.api.ApiModels.CategoryView;
import com.acme.orders.api.ApiModels.DiscountRequest;
import com.acme.orders.api.ApiModels.DiscountView;
import com.acme.orders.api.ApiModels.ProductRequest;
import com.acme.orders.api.ApiModels.ProductView;
import com.acme.orders.api.ApiModels.StockAdjustmentRequest;
import com.acme.orders.api.ApiModels.StockView;
import com.acme.orders.api.ApiModels.WarehouseRequest;
import com.acme.orders.api.ApiModels.WarehouseView;
import com.acme.orders.inventory.InventoryService;
import com.acme.orders.catalog.CatalogService;
import com.acme.orders.promotion.DiscountService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final CatalogService catalog;
    private final InventoryService inventory;
    private final DiscountService discounts;

    public AdminController(CatalogService catalog, InventoryService inventory, DiscountService discounts) {
        this.catalog = catalog;
        this.inventory = inventory;
        this.discounts = discounts;
    }

    @PostMapping("/categories")
    public CategoryView category(@Valid @RequestBody CategoryRequest request) {
        return catalog.createCategory(request);
    }

    @PutMapping("/categories/{id}")
    public CategoryView updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return catalog.updateCategory(id, request);
    }

    @PostMapping("/products")
    public ProductView product(@Valid @RequestBody ProductRequest request) {
        return catalog.createProduct(request);
    }

    @PutMapping("/products/{id}")
    public ProductView updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return catalog.updateProduct(id, request);
    }

    @DeleteMapping("/products/{id}")
    public void deactivateProduct(@PathVariable Long id) {
        catalog.deactivateProduct(id);
    }

    @PostMapping("/warehouses")
    public WarehouseView warehouse(@Valid @RequestBody WarehouseRequest request) {
        return inventory.createWarehouse(request);
    }

    @PutMapping("/inventory/products/{productId}/warehouses/{warehouseId}")
    public StockView stock(@PathVariable Long productId, @PathVariable Long warehouseId,
                           @Valid @RequestBody StockAdjustmentRequest request) {
        return inventory.setOnHand(productId, warehouseId, request.onHand());
    }

    @PostMapping("/discounts")
    public DiscountView discount(@Valid @RequestBody DiscountRequest request) {
        return discounts.create(request);
    }

    @PutMapping("/discounts/{id}/deactivate")
    public DiscountView deactivateDiscount(@PathVariable Long id) {
        return discounts.deactivate(id);
    }
}