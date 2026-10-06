package com.acme.orders.cart;

import com.acme.orders.api.ApiModels.CartLineView;
import com.acme.orders.api.ApiModels.CartView;
import com.acme.orders.catalog.Product;
import com.acme.orders.catalog.ProductRepository;
import com.acme.orders.common.BusinessRuleException;
import com.acme.orders.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class CartService {
    private final CartRepository carts;
    private final ProductRepository products;

    public CartService(CartRepository carts, ProductRepository products) {
        this.carts = carts;
        this.products = products;
    }

    @Transactional(readOnly = true)
    public CartView get(String owner) {
        return carts.findByOwner(owner).map(this::view).orElse(new CartView(List.of(), BigDecimal.ZERO));
    }

    public CartView add(String owner, Long productId, int quantity) {
        if (quantity < 1 || quantity > 99) {
            throw new BusinessRuleException("Quantity must be between 1 and 99");
        }
        Product product = products.findById(productId).filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        ShoppingCart cart = getOrCreate(owner);
        CartItem item = cart.getItems().stream().filter(existing -> existing.getProduct().getId().equals(productId)).findFirst().orElse(null);
        if (item == null) {
            item = new CartItem();
            item.setProduct(product);
            item.setQuantity(quantity);
            cart.add(item);
        } else {
            if (item.getQuantity() + quantity > 99) {
                throw new BusinessRuleException("A cart line cannot exceed 99 units");
            }
            item.setQuantity(item.getQuantity() + quantity);
        }
        return view(cart);
    }

    public CartView remove(String owner, Long productId) {
        ShoppingCart cart = carts.lockByOwner(owner)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        CartItem item = cart.getItems().stream().filter(existing -> existing.getProduct().getId().equals(productId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Product is not in the cart"));
        cart.remove(item);
        return view(cart);
    }

    private ShoppingCart getOrCreate(String owner) {
        return carts.lockByOwner(owner).orElseGet(() -> {
            ShoppingCart cart = new ShoppingCart();
            cart.setOwner(owner);
            return carts.save(cart);
        });
    }

    private CartView view(ShoppingCart cart) {
        List<CartLineView> lines = cart.getItems().stream().map(item -> new CartLineView(
                item.getProduct().getId(), item.getProduct().getName(), item.getProduct().getPrice(), item.getQuantity(),
                item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))).toList();
        BigDecimal subtotal = lines.stream().map(CartLineView::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartView(lines, subtotal);
    }
}