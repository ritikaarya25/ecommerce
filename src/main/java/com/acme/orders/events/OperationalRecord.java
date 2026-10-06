package com.acme.orders.events;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "operational_records")
@Getter
@Setter
@NoArgsConstructor
public class OperationalRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String kind;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private String detail;

    @Column(nullable = false)
    private Instant createdAt;
}