package com.acme.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class OrderManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderManagementApplication.class, args);
    }
}