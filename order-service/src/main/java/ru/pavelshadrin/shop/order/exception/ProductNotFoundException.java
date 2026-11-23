package ru.pavelshadrin.shop.order.exception;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(UUID productId) {
        super("Товар не найден: " + productId);
    }
}
