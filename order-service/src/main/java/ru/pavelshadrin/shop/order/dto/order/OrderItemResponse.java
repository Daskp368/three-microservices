package ru.pavelshadrin.shop.order.dto.order;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID productId,
        String name,
        int quantity,
        BigDecimal price,
        BigDecimal sale,
        BigDecimal discountedUnitPrice,
        BigDecimal totalPrice
) {
}
