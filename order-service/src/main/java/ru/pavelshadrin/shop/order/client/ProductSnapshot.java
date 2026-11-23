package ru.pavelshadrin.shop.order.client;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSnapshot (
    UUID productId,
    String name,
    int availableQuantity,
    BigDecimal price,
    BigDecimal sale,
    boolean available
) {

}
