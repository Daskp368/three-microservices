package ru.pavelshadrin.shop.order.service;

import ru.pavelshadrin.shop.order.client.ProductSnapshot;

import java.math.BigDecimal;

public record CalculatedOrderItem(
        ProductSnapshot product,
        int quantity,
        BigDecimal discountedUnitPrice,
        BigDecimal totalPrice
) {
}
