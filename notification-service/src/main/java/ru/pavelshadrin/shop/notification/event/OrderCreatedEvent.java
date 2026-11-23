package ru.pavelshadrin.shop.notification.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        @NotNull UUID eventId,
        @NotNull Integer eventVersion,
        @NotNull Instant occurredAt,
        @NotNull UUID orderId,
        @NotNull UUID userId,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal totalPrice,
        @NotEmpty @Valid List<OrderCreatedItemEvent> items
) {
    public OrderCreatedEvent {
        if (items != null) {
            items = List.copyOf(items);
        }
    }
}
