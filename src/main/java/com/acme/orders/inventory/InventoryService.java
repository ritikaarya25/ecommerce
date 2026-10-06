package com.acme.orders.inventory;

import com.acme.orders.api.ApiModels.StockView;
import com.acme.orders.api.ApiModels.WarehouseRequest;
import com.acme.orders.api.ApiModels.WarehouseView;
import com.acme.orders.catalog.Product;
import com.acme.orders.catalog.ProductRepository;
import com.acme.orders.common.BusinessRuleException;
import com.acme.orders.common.ResourceNotFoundException;
import com.acme.orders.orders.OrderLine;
import com.acme.orders.orders.InventoryReservation;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class InventoryService {
    @PersistenceContext
    private EntityManager entityManager;

    private final WarehouseRepository warehouses;
    private final StockRepository stockRepository;
    private final ProductRepository products;

    public InventoryService(WarehouseRepository warehouses, StockRepository stockRepository, ProductRepository products) {
        this.warehouses = warehouses;
        this.stockRepository = stockRepository;
        this.products = products;
    }

    public WarehouseView createWarehouse(WarehouseRequest request) {
        Warehouse warehouse = new Warehouse();
        warehouse.setName(request.name().trim());
        warehouse.setLocation(request.location().trim());
        warehouse = warehouses.save(warehouse);
        return new WarehouseView(warehouse.getId(), warehouse.getName(), warehouse.getLocation(), warehouse.isActive());
    }

    public StockView setOnHand(Long productId, Long warehouseId, int onHand) {
        if (onHand < 0) {
            throw new BusinessRuleException("On-hand inventory cannot be negative");
        }
        Product product = products.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        Warehouse warehouse = warehouses.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + warehouseId));
        Stock stock = stockRepository.lockByProductAndWarehouse(productId, warehouseId).orElseGet(() -> {
            Stock created = new Stock();
            created.setProduct(product);
            created.setWarehouse(warehouse);
            created.setOnHand(0);
            created.setReserved(0);
            return created;
        });
        if (onHand < stock.getReserved()) {
            throw new BusinessRuleException("On-hand inventory cannot be lower than reserved inventory");
        }
        stock.setOnHand(onHand);
        return view(stockRepository.save(stock));
    }

    @Transactional(readOnly = true)
    public List<StockView> warehouseStock(Long warehouseId) {
        if (!warehouses.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Warehouse not found: " + warehouseId);
        }
        return stockRepository.findByWarehouseIdOrderByProductId(warehouseId).stream().map(this::view).toList();
    }

    public List<Allocation> reserve(Long productId, int quantity) {
        List<Stock> candidates = stockRepository.lockByProductId(productId);
        int available = candidates.stream().mapToInt(Stock::available).sum();
        if (available < quantity) {
            throw new BusinessRuleException("Insufficient inventory for product " + productId);
        }
        int remaining = quantity;
        List<Allocation> allocations = new ArrayList<>();
        for (Stock stock : candidates) {
            int allocation = Math.min(remaining, stock.available());
            if (allocation > 0) {
                stock.setReserved(stock.getReserved() + allocation);
                allocations.add(new Allocation(stock, allocation));
                remaining -= allocation;
            }
            if (remaining == 0) {
                break;
            }
        }
        return allocations;
    }

    public record Allocation(Stock stock, int quantity) {
    }

    public void markShipped(List<OrderLine> lines) {
        List<InventoryReservation> reservations = lockReservations(lines);
        for (var reservation : reservations) {
            if (!reservation.isFulfilled()) {
                Stock stock = reservation.getStock();
                stock.setReserved(stock.getReserved() - reservation.getQuantity());
                stock.setOnHand(stock.getOnHand() - reservation.getQuantity());
                reservation.setFulfilled(true);
            }
        }
    }

    public void restock(List<OrderLine> lines) {
        List<InventoryReservation> reservations = lockReservations(lines);
        for (var reservation : reservations) {
            Stock stock = reservation.getStock();
            if (reservation.isFulfilled()) {
                stock.setOnHand(stock.getOnHand() + reservation.getQuantity());
                reservation.setFulfilled(false);
            } else {
                stock.setReserved(stock.getReserved() - reservation.getQuantity());
            }
        }
    }

    private List<InventoryReservation> lockReservations(List<OrderLine> lines) {
        List<InventoryReservation> reservations = lines.stream().flatMap(line -> line.getReservations().stream()).toList();
        if (reservations.isEmpty()) {
            return reservations;
        }
        List<Long> stockIds = reservations.stream().map(reservation -> reservation.getStock().getId()).distinct().sorted().toList();
        stockRepository.lockByIds(stockIds).forEach(stock -> entityManager.refresh(stock, LockModeType.PESSIMISTIC_WRITE));
        return reservations;
    }

    private StockView view(Stock stock) {
        return new StockView(stock.getProduct().getId(), stock.getProduct().getName(), stock.getWarehouse().getId(),
                stock.getWarehouse().getName(), stock.getOnHand(), stock.getReserved(), stock.available());
    }
}