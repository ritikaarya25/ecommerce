package com.acme.orders.api;

import com.acme.orders.api.ApiModels.CategoryView;
import com.acme.orders.api.ApiModels.ProductView;
import com.acme.orders.catalog.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {
    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/categories")
    public List<CategoryView> categories() {
        return catalog.categories();
    }

    @GetMapping("/products")
    public List<ProductView> products(@RequestParam(required = false) Long categoryId) {
        return catalog.products(categoryId);
    }

    @GetMapping("/products/{id}")
    public ProductView product(@PathVariable Long id) {
        return catalog.product(id);
    }
}