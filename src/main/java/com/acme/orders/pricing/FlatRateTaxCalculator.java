package com.acme.orders.pricing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class FlatRateTaxCalculator implements TaxCalculator {
    private final BigDecimal rate;

    public FlatRateTaxCalculator(@Value("${app.tax-rate:0.08}") BigDecimal rate) {
        this.rate = rate;
    }

    @Override
    public BigDecimal calculate(BigDecimal taxableAmount) {
        return taxableAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}