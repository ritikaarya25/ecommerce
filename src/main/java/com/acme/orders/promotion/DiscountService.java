package com.acme.orders.promotion;

import com.acme.orders.api.ApiModels.DiscountRequest;
import com.acme.orders.api.ApiModels.DiscountView;
import com.acme.orders.common.BusinessRuleException;
import com.acme.orders.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
@Transactional
public class DiscountService {
    private final DiscountRepository discounts;

    public DiscountService(DiscountRepository discounts) {
        this.discounts = discounts;
    }

    public DiscountView create(DiscountRequest request) {
        if (request.type() == DiscountType.PERCENTAGE && request.value().compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessRuleException("Percentage discounts cannot exceed 100");
        }
        Discount discount = new Discount();
        discount.setCode(request.code().trim().toUpperCase());
        discount.setType(request.type());
        discount.setValue(request.value());
        discount.setExpiresAt(request.expiresAt());
        discount.setMaxUses(request.maxUses());
        return view(discounts.save(discount));
    }

    public BigDecimal redeem(String code, BigDecimal subtotal) {
        if (code == null || code.isBlank()) {
            return BigDecimal.ZERO;
        }
        Discount discount = discounts.lockByCode(code.trim())
                .orElseThrow(() -> new BusinessRuleException("Discount code is invalid"));
        if (!discount.isActive() || (discount.getExpiresAt() != null && !discount.getExpiresAt().isAfter(Instant.now()))
                || (discount.getMaxUses() != null && discount.getUses() >= discount.getMaxUses())) {
            throw new BusinessRuleException("Discount code is expired or no longer available");
        }
        BigDecimal value = discount.getType() == DiscountType.PERCENTAGE
                ? subtotal.multiply(discount.getValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                : discount.getValue();
        discount.setUses(discount.getUses() + 1);
        return value.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }

    public DiscountView deactivate(Long id) {
        Discount discount = discounts.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found: " + id));
        discount.setActive(false);
        return view(discount);
    }

    private DiscountView view(Discount discount) {
        return new DiscountView(discount.getId(), discount.getCode(), discount.getType(), discount.getValue(),
                discount.getExpiresAt(), discount.getMaxUses(), discount.getUses(), discount.isActive());
    }
}