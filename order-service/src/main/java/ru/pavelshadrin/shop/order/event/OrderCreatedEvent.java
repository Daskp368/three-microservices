package ru.pavelshadrin.shop.order.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        int eventVersion,
        Instant occurredAt,
        UUID orderId,
        UUID userId,
        BigDecimal totalPrice,
        List<OrderCreatedItemEvent> items
) {
    public OrderCreatedEvent {
        items = List.copyOf(items);
    }
}
