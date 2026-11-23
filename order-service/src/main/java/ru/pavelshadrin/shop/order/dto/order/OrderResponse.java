package ru.pavelshadrin.shop.order.dto.order;

import ru.pavelshadrin.shop.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        UUID userId,
        OrderStatus status,
        Instant createdAt,
        List<OrderItemResponse> items,
        BigDecimal totalPrice
) {
    public OrderResponse {
        items = List.copyOf(items);
    }
}
