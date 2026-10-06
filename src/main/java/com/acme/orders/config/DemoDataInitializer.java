package com.acme.orders.config;

import com.acme.orders.catalog.Category;
import com.acme.orders.catalog.CategoryRepository;
import com.acme.orders.catalog.Product;
import com.acme.orders.catalog.ProductRepository;
import com.acme.orders.inventory.Stock;
import com.acme.orders.inventory.StockRepository;
import com.acme.orders.inventory.Warehouse;
import com.acme.orders.inventory.WarehouseRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class DemoDataInitializer {
    @Bean
    CommandLineRunner seedDemoData(CategoryRepository categories, ProductRepository products,
                                   WarehouseRepository warehouses, StockRepository stockRepository) {
        return args -> {
            if (categories.count() > 0) {
                return;
            }
            Category electronics = new Category();
            electronics.setName("Electronics");
            electronics.setDescription("Devices and accessories");
            categories.save(electronics);

            Warehouse west = warehouse("West Hub", "San Francisco", warehouses);
            Warehouse east = warehouse("East Hub", "New York", warehouses);
            Product headphones = product("AUDIO-100", "Wireless Headphones", "Over-ear headphones", "79.99", electronics, products);
            Product speaker = product("AUDIO-200", "Portable Speaker", "Water-resistant speaker", "49.50", electronics, products);
            stock(headphones, west, 8, stockRepository);
            stock(headphones, east, 6, stockRepository);
            stock(speaker, west, 4, stockRepository);
            stock(speaker, east, 7, stockRepository);
        };
    }

    private Warehouse warehouse(String name, String location, WarehouseRepository repository) {
        Warehouse warehouse = new Warehouse();
        warehouse.setName(name);
        warehouse.setLocation(location);
        return repository.save(warehouse);
    }

    private Product product(String sku, String name, String description, String price,
                            Category category, ProductRepository repository) {
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setCategory(category);
        return repository.save(product);
    }

    private void stock(Product product, Warehouse warehouse, int onHand, StockRepository repository) {
        Stock stock = new Stock();
        stock.setProduct(product);
        stock.setWarehouse(warehouse);
        stock.setOnHand(onHand);
        stock.setReserved(0);
        repository.save(stock);
    }
}