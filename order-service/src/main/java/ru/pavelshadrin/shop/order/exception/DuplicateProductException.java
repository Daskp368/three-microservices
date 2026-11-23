package ru.pavelshadrin.shop.order.exception;

import java.util.UUID;

public class DuplicateProductException extends RuntimeException {

    public DuplicateProductException(UUID productId) {
        super("Товар повторяется в заказе: " + productId);
    }
}
