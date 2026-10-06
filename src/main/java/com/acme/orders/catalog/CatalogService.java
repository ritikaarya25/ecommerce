package com.acme.orders.catalog;

import com.acme.orders.api.ApiModels.CategoryRequest;
import com.acme.orders.api.ApiModels.CategoryView;
import com.acme.orders.api.ApiModels.ProductRequest;
import com.acme.orders.api.ApiModels.ProductView;
import com.acme.orders.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CatalogService {
    private final CategoryRepository categories;
    private final ProductRepository products;

    public CatalogService(CategoryRepository categories, ProductRepository products) {
        this.categories = categories;
        this.products = products;
    }

    @Transactional(readOnly = true)
    public List<CategoryView> categories() {
        return categories.findAll().stream().map(this::view).toList();
    }

    public CategoryView createCategory(CategoryRequest request) {
        Category category = new Category();
        category.setName(request.name().trim());
        category.setDescription(request.description());
        return view(categories.save(category));
    }

    public CategoryView updateCategory(Long id, CategoryRequest request) {
        Category category = categories.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        category.setName(request.name().trim());
        category.setDescription(request.description());
        return view(category);
    }

    @Transactional(readOnly = true)
    public List<ProductView> products(Long categoryId) {
        List<Product> results = categoryId == null ? products.findByActiveTrue() : products.findByCategoryIdAndActiveTrue(categoryId);
        return results.stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public ProductView product(Long id) {
        return view(products.findById(id).filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id)));
    }

    public ProductView createProduct(ProductRequest request) {
        Category category = categories.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.categoryId()));
        Product product = new Product();
        product.setSku(request.sku().trim());
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setCategory(category);
        return view(products.save(product));
    }

    public ProductView updateProduct(Long id, ProductRequest request) {
        Product product = products.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        Category category = categories.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.categoryId()));
        product.setSku(request.sku().trim());
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setCategory(category);
        return view(product);
    }

    public void deactivateProduct(Long id) {
        Product product = products.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        product.setActive(false);
    }

    private CategoryView view(Category category) {
        return new CategoryView(category.getId(), category.getName(), category.getDescription());
    }

    private ProductView view(Product product) {
        return new ProductView(product.getId(), product.getSku(), product.getName(), product.getDescription(),
                product.getPrice(), product.getCategory().getId(), product.getCategory().getName());
    }
}