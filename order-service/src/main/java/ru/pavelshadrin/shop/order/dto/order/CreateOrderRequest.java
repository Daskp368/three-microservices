package ru.pavelshadrin.shop.order.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderRequest(
        @NotEmpty(message = "Заказ должен содержать минимум одну позицию")
        List<@Valid CreateOrderItemRequest> items
) {
    public CreateOrderRequest {
        if (items != null) {
            items = List.copyOf(items);
        }
    }
}
