package com.acme.orders.api;

import com.acme.orders.orders.OrderStatus;
import com.acme.orders.payment.PaymentStatus;
import com.acme.orders.promotion.DiscountType;
import com.acme.orders.returns.ReturnStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ApiModels {
    private ApiModels() {
    }

    public record CategoryRequest(@NotBlank String name, String description) {
    }

    public record CategoryView(Long id, String name, String description) {
    }

    public record ProductRequest(@NotBlank String sku, @NotBlank String name, String description,
                                 @NotNull @DecimalMin("0.01") BigDecimal price, @NotNull Long categoryId) {
    }

    public record ProductView(Long id, String sku, String name, String description,
                              BigDecimal price, Long categoryId, String categoryName) {
    }

    public record WarehouseRequest(@NotBlank String name, @NotBlank String location) {
    }

    public record WarehouseView(Long id, String name, String location, boolean active) {
    }

    public record StockAdjustmentRequest(@Min(0) int onHand) {
    }

    public record StockView(Long productId, String productName, Long warehouseId, String warehouseName,
                            int onHand, int reserved, int available) {
    }

    public record CartItemRequest(@NotNull Long productId, @Min(1) @Max(99) int quantity) {
    }

    public record CartLineView(Long productId, String productName, BigDecimal unitPrice, int quantity,
                               BigDecimal lineTotal) {
    }

    public record CartView(List<CartLineView> items, BigDecimal subtotal) {
    }

    public record CheckoutRequest(@NotBlank @Size(max = 500) String shippingAddress,
                                  @Size(max = 80) String discountCode, @NotBlank String paymentToken) {
    }

    public record OrderLineView(Long productId, String productName, int quantity, BigDecimal unitPrice,
                                List<AllocationView> allocations) {
    }

    public record AllocationView(Long warehouseId, String warehouseName, int quantity) {
    }

    public record OrderView(Long id, OrderStatus status, String shippingAddress, BigDecimal subtotal,
                            BigDecimal discountAmount, BigDecimal taxAmount, BigDecimal total,
                            PaymentStatus paymentStatus, Instant createdAt, List<OrderLineView> lines) {
    }

    public record ReturnRequestBody(@NotBlank @Size(max = 500) String reason) {
    }

    public record ReturnDecision(@NotNull ReturnStatus status) {
    }

    public record ReturnView(Long id, Long orderId, String reason, ReturnStatus status, Instant createdAt) {
    }

    public record DiscountRequest(@NotBlank String code, @NotNull DiscountType type,
                                  @NotNull @Positive BigDecimal value, Instant expiresAt,
                                  @Positive Integer maxUses) {
    }

    public record DiscountView(Long id, String code, DiscountType type, BigDecimal value,
                               Instant expiresAt, Integer maxUses, int uses, boolean active) {
    }

    public record OperationalRecordView(Long id, String kind, Long orderId, String detail, Instant createdAt) {
    }
}