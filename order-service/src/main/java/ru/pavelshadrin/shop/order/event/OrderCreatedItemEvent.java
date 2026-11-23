package ru.pavelshadrin.shop.order.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreatedItemEvent(
        UUID productId,
        int quantity,
        BigDecimal price,
        BigDecimal sale,
        BigDecimal totalPrice
) {
}
