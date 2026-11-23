package ru.pavelshadrin.shop.order.dto.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CreateOrderItemRequest(
        @NotNull(message = "productId обязателен")
        UUID productId,

        @NotNull(message = "quantity обязателен")
        @Positive(message = "quantity должен быть больше нуля")
        Integer quantity
) {
}
