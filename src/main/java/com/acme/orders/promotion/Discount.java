package com.acme.orders.promotion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "discounts")
@Getter
@Setter
@NoArgsConstructor
public class Discount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String code;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType type;

    @NotNull
    @DecimalMin("0.01")
    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    private Instant expiresAt;

    @Positive
    private Integer maxUses;

    @Column(nullable = false)
    private int uses;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    private long version;
}