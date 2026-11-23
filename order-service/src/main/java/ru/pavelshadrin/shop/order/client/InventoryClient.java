package ru.pavelshadrin.shop.order.client;

import java.util.UUID;

public interface InventoryClient {

    ProductSnapshot checkAvailability(UUID productId, int requestedQuantity);
}
