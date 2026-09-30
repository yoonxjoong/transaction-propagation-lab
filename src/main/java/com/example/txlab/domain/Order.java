package com.example.txlab.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;

    protected Order() {
    }

    public Order(String productName) {
        this.productName = productName;
    }

    public Long getId() {
        return id;
    }

    public String getProductName() {
        return productName;
    }
}
