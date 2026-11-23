package ru.pavelshadrin.shop.notification.dto.order;

import java.math.BigDecimal;
import java.util.UUID;

public record NotificationOrderResponse(
        UUID id,
        UUID orderId,
        UUID productId,
        int quantity,
        BigDecimal price,
        BigDecimal sale,
        BigDecimal totalPrice,
        UUID userId
) {
}
