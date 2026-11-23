package ru.pavelshadrin.shop.order.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.pavelshadrin.shop.order.client.InventoryClient;
import ru.pavelshadrin.shop.order.client.ProductSnapshot;
import ru.pavelshadrin.shop.order.dto.order.CreateOrderItemRequest;
import ru.pavelshadrin.shop.order.dto.order.CreateOrderRequest;
import ru.pavelshadrin.shop.order.dto.order.OrderResponse;
import ru.pavelshadrin.shop.order.exception.DuplicateProductException;
import ru.pavelshadrin.shop.order.exception.InsufficientStockException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCreationServiceTests {

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private OrderPriceCalculator priceCalculator;

    @Mock
    private OrderPersistenceService persistenceService;

    @InjectMocks
    private OrderCreationService service;

    @Test
    void checkEveryProductBeforeSavingCalculatedOrder() {
        UUID userId = UUID.randomUUID();
        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();
        ProductSnapshot firstProduct = product(firstProductId, "1200.00", "15.00", true);
        ProductSnapshot secondProduct = product(secondProductId, "499.90", "0.00", true);
        CalculatedOrderItem firstItem = calculated(firstProduct, 2, "1020.00", "2040.00");
        CalculatedOrderItem secondItem = calculated(secondProduct, 3, "499.90", "1499.70");
        CreateOrderRequest request = request(
                new CreateOrderItemRequest(firstProductId, 2),
                new CreateOrderItemRequest(secondProductId, 3)
        );

        when(inventoryClient.checkAvailability(firstProductId, 2)).thenReturn(firstProduct);
        when(inventoryClient.checkAvailability(secondProductId, 3)).thenReturn(secondProduct);
        when(priceCalculator.calculate(firstProduct, 2)).thenReturn(firstItem);
        when(priceCalculator.calculate(secondProduct, 3)).thenReturn(secondItem);

        service.create(userId, request);

        verify(persistenceService).save(
                eq(userId),
                eq(List.of(firstItem, secondItem)),
                eq(new BigDecimal("3539.70"))
        );
    }

    @Test
    void rejectDuplicateBeforeCallingInventory() {
        UUID productId = UUID.randomUUID();
        CreateOrderRequest request = request(
                new CreateOrderItemRequest(productId, 1),
                new CreateOrderItemRequest(productId, 2)
        );

        assertThatThrownBy(() -> service.create(UUID.randomUUID(), request))
                .isInstanceOf(DuplicateProductException.class);

        verifyNoInteractions(inventoryClient, priceCalculator, persistenceService);
    }

    @Test
    void doNotSaveWhenStockIsInsufficient() {
        UUID productId = UUID.randomUUID();
        ProductSnapshot product = product(productId, "100.00", "0.00", false);
        when(inventoryClient.checkAvailability(productId, 2)).thenReturn(product);

        assertThatThrownBy(() -> service.create(
                UUID.randomUUID(),
                request(new CreateOrderItemRequest(productId, 2))
        )).isInstanceOf(InsufficientStockException.class);

        verify(priceCalculator, never()).calculate(product, 2);
        verify(persistenceService, never()).save(
                org.mockito.ArgumentMatchers.any(),
                anyList(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private CreateOrderRequest request(CreateOrderItemRequest... items) {
        return new CreateOrderRequest(List.of(items));
    }

    private ProductSnapshot product(
            UUID productId,
            String price,
            String sale,
            boolean available
    ) {
        return new ProductSnapshot(
                productId,
                "Товар",
                available ? 10 : 1,
                new BigDecimal(price),
                new BigDecimal(sale),
                available
        );
    }

    private CalculatedOrderItem calculated(
            ProductSnapshot product,
            int quantity,
            String discountedPrice,
            String totalPrice
    ) {
        return new CalculatedOrderItem(
                product,
                quantity,
                new BigDecimal(discountedPrice),
                new BigDecimal(totalPrice)
        );
    }
}
