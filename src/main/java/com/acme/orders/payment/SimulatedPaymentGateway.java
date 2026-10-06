package com.acme.orders.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class SimulatedPaymentGateway implements PaymentGateway {
    @Override
    public ChargeResult charge(BigDecimal amount, String paymentToken) {
        if ("decline".equalsIgnoreCase(paymentToken)) {
            return new ChargeResult(false, "DECLINED");
        }
        return new ChargeResult(true, "test-charge-" + UUID.randomUUID());
    }

    @Override
    public String refund(String providerReference, BigDecimal amount) {
        return "test-refund-" + UUID.randomUUID();
    }
}