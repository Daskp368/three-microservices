package ru.pavelshadrin.shop.order.service;

import org.springframework.stereotype.Service;
import ru.pavelshadrin.shop.order.client.InventoryClient;
import ru.pavelshadrin.shop.order.client.ProductSnapshot;
import ru.pavelshadrin.shop.order.dto.order.CreateOrderItemRequest;
import ru.pavelshadrin.shop.order.dto.order.CreateOrderRequest;
import ru.pavelshadrin.shop.order.dto.order.OrderResponse;
import ru.pavelshadrin.shop.order.exception.DuplicateProductException;
import ru.pavelshadrin.shop.order.exception.InsufficientStockException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderCreationService {

    private final InventoryClient inventoryClient;
    private final OrderPriceCalculator priceCalculator;
    private final OrderPersistenceService persistenceService;

    public OrderCreationService(
            InventoryClient inventoryClient,
            OrderPriceCalculator priceCalculator,
            OrderPersistenceService persistenceService
    ) {
        this.inventoryClient = inventoryClient;
        this.priceCalculator = priceCalculator;
        this.persistenceService = persistenceService;
    }

    public OrderResponse create(UUID userId, CreateOrderRequest request) {
        rejectDuplicateProducts(request.items());

        List<CalculatedOrderItem> calculatedItems = new ArrayList<>();
        for (CreateOrderItemRequest item : request.items()) {
            ProductSnapshot product = inventoryClient.checkAvailability(
                    item.productId(),
                    item.quantity()
            );
            if (!product.available()) {
                throw new InsufficientStockException(item.productId());
            }
            calculatedItems.add(priceCalculator.calculate(product, item.quantity()));
        }

        BigDecimal orderTotal = calculatedItems.stream()
                .map(CalculatedOrderItem::totalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return persistenceService.save(userId, calculatedItems, orderTotal);
    }

    private void rejectDuplicateProducts(List<CreateOrderItemRequest> items) {
        Set<UUID> productIds = new HashSet<>();
        for (CreateOrderItemRequest item : items) {
            if (!productIds.add(item.productId())) {
                throw new DuplicateProductException(item.productId());
            }
        }
    }
}
