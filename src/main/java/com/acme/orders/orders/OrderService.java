package com.acme.orders.orders;

import com.acme.orders.api.ApiModels.AllocationView;
import com.acme.orders.api.ApiModels.CheckoutRequest;
import com.acme.orders.api.ApiModels.OrderLineView;
import com.acme.orders.api.ApiModels.OrderView;
import com.acme.orders.cart.CartRepository;
import com.acme.orders.cart.ShoppingCart;
import com.acme.orders.common.BusinessRuleException;
import com.acme.orders.common.ResourceNotFoundException;
import com.acme.orders.events.OrderLifecycleEvent;
import com.acme.orders.events.OrderPlacedEvent;
import com.acme.orders.inventory.InventoryService;
import com.acme.orders.payment.Payment;
import com.acme.orders.payment.PaymentGateway;
import com.acme.orders.payment.PaymentRepository;
import com.acme.orders.payment.PaymentStatus;
import com.acme.orders.pricing.TaxCalculator;
import com.acme.orders.promotion.DiscountService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {
    private final CartRepository carts;
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final InventoryService inventory;
    private final DiscountService discounts;
    private final TaxCalculator taxCalculator;
    private final PaymentGateway paymentGateway;
    private final ApplicationEventPublisher events;

    public OrderService(CartRepository carts, OrderRepository orders, PaymentRepository payments,
                        InventoryService inventory, DiscountService discounts, TaxCalculator taxCalculator,
                        PaymentGateway paymentGateway, ApplicationEventPublisher events) {
        this.carts = carts;
        this.orders = orders;
        this.payments = payments;
        this.inventory = inventory;
        this.discounts = discounts;
        this.taxCalculator = taxCalculator;
        this.paymentGateway = paymentGateway;
        this.events = events;
    }

    public OrderView checkout(String username, CheckoutRequest request) {
        ShoppingCart cart = carts.lockByOwner(username).orElseThrow(() -> new BusinessRuleException("Cart is empty"));
        if (cart.getItems().isEmpty()) {
            throw new BusinessRuleException("Cart is empty");
        }
        BigDecimal subtotal = cart.getItems().stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = discounts.redeem(request.discountCode(), subtotal);
        BigDecimal taxable = subtotal.subtract(discount);
        BigDecimal tax = taxCalculator.calculate(taxable);
        BigDecimal total = taxable.add(tax).setScale(2, RoundingMode.HALF_UP);

        PurchaseOrder order = new PurchaseOrder();
        order.setCustomerUsername(username);
        order.setStatus(OrderStatus.PLACED);
        order.setSubtotal(subtotal);
        order.setDiscountAmount(discount);
        order.setTaxAmount(tax);
        order.setTotal(total);
        order.setShippingAddress(request.shippingAddress().trim());
        order.setCreatedAt(Instant.now());

        var sortedItems = cart.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId())).toList();
        for (var item : sortedItems) {
            OrderLine line = new OrderLine();
            line.setProductId(item.getProduct().getId());
            line.setProductName(item.getProduct().getName());
            line.setQuantity(item.getQuantity());
            line.setUnitPrice(item.getProduct().getPrice());
            order.addLine(line);
            List<InventoryService.Allocation> allocations = inventory.reserve(item.getProduct().getId(), item.getQuantity());
            for (InventoryService.Allocation allocation : allocations) {
                line.reserveFrom(allocation.stock(), allocation.quantity());
            }
        }

        PaymentGateway.ChargeResult charge = paymentGateway.charge(total, request.paymentToken());
        if (!charge.approved()) {
            throw new BusinessRuleException("Payment was declined");
        }
        orders.saveAndFlush(order);
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(total);
        payment.setRefundedAmount(BigDecimal.ZERO);
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setProviderReference(charge.reference());
        payments.save(payment);
        cart.getItems().clear();

        Set<Long> warehouseIds = order.getLines().stream().flatMap(line -> line.getReservations().stream())
                .map(reservation -> reservation.getStock().getWarehouse().getId()).collect(Collectors.toSet());
        events.publishEvent(new OrderPlacedEvent(order.getId(), username, warehouseIds.stream().sorted().toList()));
        return view(order, payment.getStatus());
    }

    @Transactional(readOnly = true)
    public List<OrderView> customerOrders(String username) {
        return orders.findByCustomerUsernameOrderByCreatedAtDesc(username).stream()
                .map(order -> view(order, payments.findByOrderId(order.getId()).map(Payment::getStatus).orElse(null))).toList();
    }

    @Transactional(readOnly = true)
    public OrderView customerOrder(String username, Long id) {
        PurchaseOrder order = detailedOrder(id);
        assertOwner(order, username);
        return view(order, payments.findByOrderId(id).map(Payment::getStatus).orElse(null));
    }

    public OrderView advanceStatus(Long id, OrderStatus next) {
        if (next == OrderStatus.RETURNED) {
            throw new BusinessRuleException("Returns must be approved through the return workflow");
        }
        PurchaseOrder order = orders.lockById(id).orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        order.getStatus().assertTransitionTo(next);
        if (next == OrderStatus.SHIPPED) {
            inventory.markShipped(order.getLines());
        }
        order.setStatus(next);
        events.publishEvent(new OrderLifecycleEvent(order.getId(), next.name()));
        return view(order, payments.findByOrderId(id).map(Payment::getStatus).orElse(null));
    }

    public PurchaseOrder lockOrder(Long id) {
        return orders.lockById(id).orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    public OrderView finishReturn(PurchaseOrder order) {
        order.getStatus().assertTransitionTo(OrderStatus.RETURNED);
        inventory.restock(order.getLines());
        Payment payment = payments.findByOrderId(order.getId())
                .orElseThrow(() -> new BusinessRuleException("Payment record not found"));
        paymentGateway.refund(payment.getProviderReference(), payment.getAmount());
        payment.setRefundedAmount(payment.getAmount());
        payment.setStatus(PaymentStatus.REFUNDED);
        order.setStatus(OrderStatus.RETURNED);
        events.publishEvent(new OrderLifecycleEvent(order.getId(), OrderStatus.RETURNED.name()));
        return view(order, payment.getStatus());
    }

    private PurchaseOrder detailedOrder(Long id) {
        return orders.findDetailedById(id).orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private void assertOwner(PurchaseOrder order, String username) {
        if (!order.getCustomerUsername().equals(username)) {
            throw new ResourceNotFoundException("Order not found: " + order.getId());
        }
    }

    private OrderView view(PurchaseOrder order, PaymentStatus paymentStatus) {
        List<OrderLineView> lines = order.getLines().stream().map(line -> new OrderLineView(
                line.getProductId(), line.getProductName(), line.getQuantity(), line.getUnitPrice(),
                line.getReservations().stream().map(reservation -> new AllocationView(
                        reservation.getStock().getWarehouse().getId(), reservation.getStock().getWarehouse().getName(),
                        reservation.getQuantity())).toList())).toList();
        return new OrderView(order.getId(), order.getStatus(), order.getShippingAddress(), order.getSubtotal(),
                order.getDiscountAmount(), order.getTaxAmount(), order.getTotal(), paymentStatus, order.getCreatedAt(), lines);
    }
}