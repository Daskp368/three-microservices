package ru.pavelshadrin.shop.order.exception;

import java.util.UUID;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(UUID productId) {
        super("Недостаточно товара на складе: " + productId);
    }
}
