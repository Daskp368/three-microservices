package ru.pavelshadrin.shop.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "sale", nullable = false, precision = 5, scale = 2)
    private BigDecimal sale;

    protected Product() {

    }

    public Product(String name, int quantity, BigDecimal price, BigDecimal sale) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.sale = sale;
    }

    public void update(String name, int quantity, BigDecimal price, BigDecimal sale) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.sale = sale;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getSale() {
        return sale;
    }
}
