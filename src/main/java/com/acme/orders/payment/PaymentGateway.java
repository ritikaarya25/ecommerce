package com.acme.orders.payment;

import java.math.BigDecimal;

public interface PaymentGateway {
    ChargeResult charge(BigDecimal amount, String paymentToken);
    String refund(String providerReference, BigDecimal amount);

    record ChargeResult(boolean approved, String reference) {
    }
}