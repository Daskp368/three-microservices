package ru.pavelshadrin.shop.order.client;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.stereotype.Component;
import ru.pavelshadrin.shop.contracts.inventory.v1.InventoryServiceGrpc;
import ru.pavelshadrin.shop.contracts.inventory.v1.ProductRequest;
import ru.pavelshadrin.shop.contracts.inventory.v1.ProductResponse;
import ru.pavelshadrin.shop.order.config.InventoryGrpcProperties;
import ru.pavelshadrin.shop.order.exception.InventoryRequestValidationException;
import ru.pavelshadrin.shop.order.exception.InventoryTimeoutException;
import ru.pavelshadrin.shop.order.exception.InventoryUnavailableException;
import ru.pavelshadrin.shop.order.exception.ProductNotFoundException;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class GrpcInventoryClient implements InventoryClient {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final InventoryServiceGrpc.InventoryServiceBlockingStub inventoryStub;
    private final InventoryGrpcProperties properties;

    public GrpcInventoryClient(
            InventoryServiceGrpc.InventoryServiceBlockingStub inventoryStub,
            InventoryGrpcProperties properties
    ) {
        this.inventoryStub = inventoryStub;
        this.properties = properties;
    }

    @Override
    public ProductSnapshot checkAvailability(UUID productId, int requestedQuantity) {
        if (productId == null || requestedQuantity <= 0) {
            throw new InventoryRequestValidationException();
        }

        ProductRequest request = ProductRequest.newBuilder()
                .setProductId(productId.toString())
                .setRequestedQuantity(requestedQuantity)
                .build();

        try {
            ProductResponse response = inventoryStub
                    .withDeadlineAfter(properties.getDeadline().toNanos(), TimeUnit.NANOSECONDS)
                    .checkAvailability(request);
            return toSnapshot(response, productId, requestedQuantity);
        } catch (StatusRuntimeException exception) {
            throw mapGrpcException(exception, productId);
        }
    }

    private ProductSnapshot toSnapshot(
            ProductResponse response,
            UUID requestedProductId,
            int requestedQuantity
    ) {
        try {
            UUID responseProductId = UUID.fromString(response.getProductId());
            BigDecimal price = new BigDecimal(response.getPrice());
            BigDecimal sale = new BigDecimal(response.getSale());
            int availableQuantity = response.getAvailableQuantity();
            boolean expectedAvailability = availableQuantity >= requestedQuantity;

            if (!Objects.equals(responseProductId, requestedProductId)
                    || response.getName().isBlank()
                    || availableQuantity < 0
                    || price.signum() < 0
                    || sale.signum() < 0
                    || sale.compareTo(ONE_HUNDRED) > 0
                    || response.getAvailable() != expectedAvailability) {
                throw new InventoryUnavailableException();
            }

            return new ProductSnapshot(
                    responseProductId,
                    response.getName(),
                    availableQuantity,
                    price,
                    sale,
                    response.getAvailable()
            );
        } catch (IllegalArgumentException exception) {
            throw new InventoryUnavailableException();
        }
    }

    private RuntimeException mapGrpcException(
            StatusRuntimeException exception,
            UUID productId
    ) {
        Status.Code code = exception.getStatus().getCode();
        return switch (code) {
            case INVALID_ARGUMENT -> new InventoryRequestValidationException();
            case NOT_FOUND -> new ProductNotFoundException(productId);
            case DEADLINE_EXCEEDED -> new InventoryTimeoutException();
            case UNAVAILABLE, INTERNAL -> new InventoryUnavailableException();
            default -> new InventoryUnavailableException();
        };
    }
}
