package com.acme.orders.pricing;

import java.math.BigDecimal;

public interface TaxCalculator {
    BigDecimal calculate(BigDecimal taxableAmount);
}