package com.acme.orders;

import com.acme.orders.catalog.Product;
import com.acme.orders.catalog.Category;
import com.acme.orders.catalog.CategoryRepository;
import com.acme.orders.catalog.ProductRepository;
import com.acme.orders.common.BusinessRuleException;
import com.acme.orders.inventory.InventoryService;
import com.acme.orders.inventory.StockRepository;
import com.acme.orders.inventory.Warehouse;
import com.acme.orders.inventory.WarehouseRepository;
import com.acme.orders.orders.OrderStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderFlowIntegrationTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ProductRepository products;

        @Autowired
        private CategoryRepository categories;

        @Autowired
        private WarehouseRepository warehouses;

        @Autowired
        private InventoryService inventory;

    @Autowired
    private StockRepository stock;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void checkoutFulfillmentAndReturnKeepPaymentAndStockConsistent() throws Exception {
        Product product = products.findBySku("AUDIO-100").orElseThrow();
        int initialOnHand = stock.findAll().stream().filter(item -> item.getProduct().getId().equals(product.getId()))
                .mapToInt(item -> item.getOnHand()).sum();

        mvc.perform(post("/api/cart/items").with(httpBasic("customer", "customer123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + product.getId() + ",\"quantity\":9}"))
                .andExpect(status().isOk());

        String checkout = "{\"shippingAddress\":\"12 Market Street\",\"paymentToken\":\"decline\"}";
        mvc.perform(post("/api/orders/checkout").with(httpBasic("customer", "customer123"))
                        .contentType(MediaType.APPLICATION_JSON).content(checkout))
                .andExpect(status().isBadRequest());
        assertEquals(0, stock.findAll().stream().filter(item -> item.getProduct().getId().equals(product.getId()))
                .mapToInt(item -> item.getReserved()).sum());

        MvcResult placed = mvc.perform(post("/api/orders/checkout").with(httpBasic("customer", "customer123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shippingAddress\":\"12 Market Street\",\"paymentToken\":\"approved\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.lines[0].allocations.length()").value(2))
                .andReturn();
        JsonNode placedBody = mapper.readTree(placed.getResponse().getContentAsString());
        long orderId = placedBody.get("id").asLong();
        assertEquals(9, stock.findAll().stream().filter(item -> item.getProduct().getId().equals(product.getId()))
                .mapToInt(item -> item.getReserved()).sum());

        for (OrderStatus next : new OrderStatus[]{OrderStatus.CONFIRMED, OrderStatus.PACKED,
                OrderStatus.SHIPPED, OrderStatus.DELIVERED}) {
            mvc.perform(patch("/api/warehouse/orders/{id}/status", orderId)
                            .with(httpBasic("warehouse", "warehouse123"))
                            .contentType(MediaType.APPLICATION_JSON).content("\"" + next.name() + "\""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(next.name()));
        }
        mvc.perform(patch("/api/warehouse/orders/{id}/status", orderId)
                        .with(httpBasic("warehouse", "warehouse123"))
                        .contentType(MediaType.APPLICATION_JSON).content("\"RETURNED\""))
                .andExpect(status().isBadRequest());
        assertEquals(initialOnHand - 9, stock.findAll().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId())).mapToInt(item -> item.getOnHand()).sum());

        MvcResult returnResponse = mvc.perform(post("/api/orders/{id}/returns", orderId)
                        .with(httpBasic("customer", "customer123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Changed my mind\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andReturn();
        long returnId = mapper.readTree(returnResponse.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(patch("/api/admin/returns/{id}", returnId).with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mvc.perform(get("/api/orders/{id}", orderId).with(httpBasic("customer", "customer123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.paymentStatus").value("REFUNDED"));
        assertEquals(initialOnHand, stock.findAll().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId())).mapToInt(item -> item.getOnHand()).sum());
        assertTrue(stock.findAll().stream().filter(item -> item.getProduct().getId().equals(product.getId()))
                .allMatch(item -> item.getReserved() == 0));
    }

    @Test
    void customerEndpointsRejectOtherRoles() throws Exception {
        mvc.perform(get("/api/cart").with(httpBasic("admin", "admin123")))
                .andExpect(status().isForbidden());
    }

        @Test
        void concurrentCheckoutsCannotReserveTheSameUnitsAcrossWarehouses() throws Exception {
                Category category = new Category();
                category.setName("Concurrent Test Category");
                categories.save(category);
                Product product = new Product();
                product.setSku("CONCURRENT-1");
                product.setName("Limited stock item");
                product.setPrice(new BigDecimal("10.00"));
                product.setCategory(category);
                products.save(product);
                Warehouse first = warehouse("Concurrent West");
                Warehouse second = warehouse("Concurrent East");
                addStock(product, first, 3);
                addStock(product, second, 2);

                CountDownLatch start = new CountDownLatch(1);
                var executor = Executors.newFixedThreadPool(2);
                try {
                        var firstAttempt = executor.submit(() -> reserveAfter(start, product.getId()));
                        var secondAttempt = executor.submit(() -> reserveAfter(start, product.getId()));
                        start.countDown();
                        int successes = (firstAttempt.get() ? 1 : 0) + (secondAttempt.get() ? 1 : 0);
                        assertEquals(1, successes);
                        assertEquals(4, stock.findAll().stream().filter(item -> item.getProduct().getId().equals(product.getId()))
                                        .mapToInt(item -> item.getReserved()).sum());
                        assertEquals(1, stock.findAll().stream().filter(item -> item.getProduct().getId().equals(product.getId()))
                                        .mapToInt(item -> item.getOnHand() - item.getReserved()).sum());
                } finally {
                        executor.shutdownNow();
                }
        }

        private boolean reserveAfter(CountDownLatch start, Long productId) throws InterruptedException {
                start.await();
                try {
                        inventory.reserve(productId, 4);
                        return true;
                } catch (BusinessRuleException exception) {
                        return false;
                }
        }

        private Warehouse warehouse(String name) {
                Warehouse warehouse = new Warehouse();
                warehouse.setName(name);
                warehouse.setLocation(name + " location");
                return warehouses.save(warehouse);
        }

        private void addStock(Product product, Warehouse warehouse, int quantity) {
                var entry = new com.acme.orders.inventory.Stock();
                entry.setProduct(product);
                entry.setWarehouse(warehouse);
                entry.setOnHand(quantity);
                entry.setReserved(0);
                stock.save(entry);
        }
}