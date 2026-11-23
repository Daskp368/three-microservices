package ru.pavelshadrin.shop.inventory.dto.product;

import java.math.BigDecimal;
import java.util.UUID;

public class ProductResponse {

    private final UUID id;
    private final String name;
    private final int quantity;
    private final BigDecimal price;
    private final BigDecimal sale;

    public ProductResponse(UUID id, String name, int quantity, BigDecimal price, BigDecimal sale) {
        this.id = id;
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
